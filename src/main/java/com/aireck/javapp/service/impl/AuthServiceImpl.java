package com.aireck.javapp.service.impl;

import com.aireck.javapp.dto.LoginRequest;
import com.aireck.javapp.dto.RefreshTokenRequest;
import com.aireck.javapp.dto.RegisterRequest;
import com.aireck.javapp.dto.StudentProfileResponse;
import com.aireck.javapp.dto.TokenResponse;
import com.aireck.javapp.dto.UserResponse;
import com.aireck.javapp.model.RefreshToken;
import com.aireck.javapp.model.Role;
import com.aireck.javapp.model.StudentProfile;
import com.aireck.javapp.model.User;
import com.aireck.javapp.model.UserRole;
import com.aireck.javapp.repository.RefreshTokenRepository;
import com.aireck.javapp.repository.RoleRepository;
import com.aireck.javapp.repository.StudentProfileRepository;
import com.aireck.javapp.repository.UserRepository;
import com.aireck.javapp.repository.UserRoleRepository;
import com.aireck.javapp.security.JwtTokenService;
import com.aireck.javapp.service.AuthService;
import com.aireck.javapp.service.PasswordPolicy;
import com.aireck.javapp.service.exception.DuplicateEmailException;
import com.aireck.javapp.service.exception.InvalidCredentialsException;
import com.aireck.javapp.service.exception.InvalidRefreshTokenException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AuthServiceImpl implements AuthService {

  private static final String STUDENT_ROLE = "student";
  private static final Duration REFRESH_TOKEN_TTL = Duration.ofDays(30);
  private static final int REFRESH_TOKEN_BYTES = 32;

  private final UserRepository userRepository;
  private final RoleRepository roleRepository;
  private final UserRoleRepository userRoleRepository;
  private final StudentProfileRepository studentProfileRepository;
  private final RefreshTokenRepository refreshTokenRepository;
  private final PasswordEncoder passwordEncoder;
  private final JwtTokenService jwtTokenService;
  private final Clock clock;
  private final SecureRandom secureRandom = new SecureRandom();

  @Autowired
  public AuthServiceImpl(
      UserRepository userRepository,
      RoleRepository roleRepository,
      UserRoleRepository userRoleRepository,
      StudentProfileRepository studentProfileRepository,
      RefreshTokenRepository refreshTokenRepository,
      PasswordEncoder passwordEncoder,
      JwtTokenService jwtTokenService) {
    this(
        userRepository,
        roleRepository,
        userRoleRepository,
        studentProfileRepository,
        refreshTokenRepository,
        passwordEncoder,
        jwtTokenService,
        Clock.systemUTC());
  }

  public AuthServiceImpl(
      UserRepository userRepository,
      RoleRepository roleRepository,
      UserRoleRepository userRoleRepository,
      StudentProfileRepository studentProfileRepository,
      RefreshTokenRepository refreshTokenRepository,
      PasswordEncoder passwordEncoder,
      JwtTokenService jwtTokenService,
      Clock clock) {
    this.userRepository = Objects.requireNonNull(userRepository);
    this.roleRepository = Objects.requireNonNull(roleRepository);
    this.userRoleRepository = Objects.requireNonNull(userRoleRepository);
    this.studentProfileRepository = Objects.requireNonNull(studentProfileRepository);
    this.refreshTokenRepository = Objects.requireNonNull(refreshTokenRepository);
    this.passwordEncoder = Objects.requireNonNull(passwordEncoder);
    this.jwtTokenService = Objects.requireNonNull(jwtTokenService);
    this.clock = Objects.requireNonNull(clock);
  }

  @Override
  @Transactional
  public UserResponse register(RegisterRequest request) {
    PasswordPolicy.validate(request.password());
    String normalizedEmail = normalizeEmail(request.email());
    if (userRepository.existsByEmail(normalizedEmail)) {
      throw new DuplicateEmailException();
    }

    Role studentRole =
        roleRepository
            .findByName(STUDENT_ROLE)
            .orElseThrow(
                () -> new IllegalStateException("The required student role is not seeded"));

    User user =
        userRepository.save(
            new User(
                request.name().trim(),
                request.lastname().trim(),
                normalizedEmail,
                passwordEncoder.encode(request.password())));

    UserRole roleAssignment = new UserRole(user, studentRole);
    user.getUserRoles().add(roleAssignment);
    userRoleRepository.save(roleAssignment);

    StudentProfile profile =
        studentProfileRepository.save(
            new StudentProfile(
                user,
                request.studentProfile().studentCode().trim(),
                request.studentProfile().major().trim()));

    return toUserResponse(user, Set.of(STUDENT_ROLE), profile);
  }

  @Override
  @Transactional
  public TokenResponse login(LoginRequest request) {
    String normalizedEmail = normalizeEmail(request.email());
    User user =
        userRepository
            .findByEmail(normalizedEmail)
            .filter(User::isActive)
            .orElseThrow(InvalidCredentialsException::new);

    if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
      throw new InvalidCredentialsException();
    }

    List<String> roles =
        userRoleRepository.findAllByUser_Id(user.getId()).stream()
            .map(UserRole::getRole)
            .map(Role::getName)
            .distinct()
            .sorted()
            .toList();

    user.setLastLoginAt(clock.instant());
    String accessToken = jwtTokenService.issueAccessToken(user.getId().toString(), roles);
    String refreshToken = createAndPersistRefreshToken(user);

    return new TokenResponse(
        accessToken, refreshToken, "Bearer", JwtTokenService.ACCESS_TOKEN_TTL.toSeconds());
  }

  @Override
  @Transactional
  public TokenResponse refresh(RefreshTokenRequest request) {
    RefreshToken currentToken = requireUsableRefreshToken(request.refreshToken());
    User user = currentToken.getUser();
    currentToken.setRevoked(true);

    List<String> roles =
        userRoleRepository.findAllByUser_Id(user.getId()).stream()
            .map(UserRole::getRole)
            .map(Role::getName)
            .distinct()
            .sorted()
            .toList();

    String accessToken = jwtTokenService.issueAccessToken(user.getId().toString(), roles);
    String refreshToken = createAndPersistRefreshToken(user);
    return new TokenResponse(
        accessToken, refreshToken, "Bearer", JwtTokenService.ACCESS_TOKEN_TTL.toSeconds());
  }

  @Override
  @Transactional
  public void logout(RefreshTokenRequest request) {
    refreshTokenRepository
        .findForUpdateByTokenHash(hashRefreshToken(request.refreshToken()))
        .filter(token -> !token.isRevoked())
        .ifPresent(token -> token.setRevoked(true));
  }

  private RefreshToken requireUsableRefreshToken(String rawToken) {
    return refreshTokenRepository
        .findForUpdateByTokenHash(hashRefreshToken(rawToken))
        .filter(token -> !token.isRevoked())
        .filter(token -> token.getExpiresAt().isAfter(clock.instant()))
        .filter(token -> token.getUser().isActive())
        .orElseThrow(InvalidRefreshTokenException::new);
  }

  private String createAndPersistRefreshToken(User user) {
    byte[] randomToken = new byte[REFRESH_TOKEN_BYTES];
    secureRandom.nextBytes(randomToken);
    String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(randomToken);
    refreshTokenRepository.save(
        new RefreshToken(
            user, hashRefreshToken(rawToken), null, clock.instant().plus(REFRESH_TOKEN_TTL)));
    return rawToken;
  }

  private String hashRefreshToken(String rawToken) {
    try {
      byte[] hash =
          MessageDigest.getInstance("SHA-256").digest(rawToken.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(hash);
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException("SHA-256 is unavailable", exception);
    }
  }

  private String normalizeEmail(String email) {
    return email.trim().toLowerCase(Locale.ROOT);
  }

  private UserResponse toUserResponse(User user, Set<String> roles, StudentProfile profile) {
    StudentProfileResponse profileResponse =
        new StudentProfileResponse(
            profile.getUserId(),
            profile.getStudentCode(),
            profile.getMajor(),
            profile.getCreatedAt(),
            profile.getUpdatedAt());

    return new UserResponse(
        user.getId(),
        user.getName(),
        user.getLastname(),
        user.getEmail(),
        user.getDocumentType(),
        user.getDocumentNumber(),
        user.getBirthDate(),
        user.getPhoneNumber(),
        user.getAddress(),
        user.isActive(),
        roles,
        profileResponse,
        user.getCreatedAt(),
        user.getUpdatedAt());
  }
}
