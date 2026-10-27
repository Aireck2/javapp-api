package com.aireck.javapp.service.impl;

import com.aireck.javapp.dto.StudentProfileResponse;
import com.aireck.javapp.dto.UserCreateRequest;
import com.aireck.javapp.dto.UserResponse;
import com.aireck.javapp.dto.UserUpdateRequest;
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
import com.aireck.javapp.service.PasswordPolicy;
import com.aireck.javapp.service.UserService;
import com.aireck.javapp.service.exception.DuplicateDocumentException;
import com.aireck.javapp.service.exception.DuplicateEmailException;
import com.aireck.javapp.service.exception.InvalidRoleException;
import com.aireck.javapp.service.exception.ResourceNotFoundException;
import java.time.Clock;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {

  private final UserRepository userRepository;
  private final RoleRepository roleRepository;
  private final UserRoleRepository userRoleRepository;
  private final StudentProfileRepository studentProfileRepository;
  private final RefreshTokenRepository refreshTokenRepository;
  private final PasswordEncoder passwordEncoder;
  private final Clock clock;

  @Autowired
  public UserServiceImpl(
      UserRepository userRepository,
      RoleRepository roleRepository,
      UserRoleRepository userRoleRepository,
      StudentProfileRepository studentProfileRepository,
      RefreshTokenRepository refreshTokenRepository,
      PasswordEncoder passwordEncoder) {
    this(
        userRepository,
        roleRepository,
        userRoleRepository,
        studentProfileRepository,
        refreshTokenRepository,
        passwordEncoder,
        Clock.systemUTC());
  }

  public UserServiceImpl(
      UserRepository userRepository,
      RoleRepository roleRepository,
      UserRoleRepository userRoleRepository,
      StudentProfileRepository studentProfileRepository,
      RefreshTokenRepository refreshTokenRepository,
      PasswordEncoder passwordEncoder,
      Clock clock) {
    this.userRepository = Objects.requireNonNull(userRepository);
    this.roleRepository = Objects.requireNonNull(roleRepository);
    this.userRoleRepository = Objects.requireNonNull(userRoleRepository);
    this.studentProfileRepository = Objects.requireNonNull(studentProfileRepository);
    this.refreshTokenRepository = Objects.requireNonNull(refreshTokenRepository);
    this.passwordEncoder = Objects.requireNonNull(passwordEncoder);
    this.clock = Objects.requireNonNull(clock);
  }

  @Override
  public Page<UserResponse> findAll(Pageable pageable) {
    return userRepository.findAll(pageable).map(this::toStoredUserResponse);
  }

  @Override
  @Transactional
  public UserResponse create(UserCreateRequest request) {
    PasswordPolicy.validate(request.password());
    String email = normalizeEmail(request.email());
    if (userRepository.existsByEmail(email)) {
      throw new DuplicateEmailException();
    }

    String documentType = normalizeOptional(request.documentType());
    String documentNumber = normalizeOptional(request.documentNumber());
    assertDocumentAvailable(documentType, documentNumber);

    Set<String> requestedRoleNames = normalizeRoles(request.roles());
    List<Role> roles = findRoles(requestedRoleNames);
    boolean isStudent = requestedRoleNames.contains("student");
    if (isStudent && request.studentProfile() == null) {
      throw new IllegalArgumentException("Student role requires an academic profile");
    }
    if (!isStudent && request.studentProfile() != null) {
      throw new IllegalArgumentException("Academic profile can only be assigned to a student");
    }

    User user =
        new User(
            requireNonBlank(request.name(), "name"),
            requireNonBlank(request.lastname(), "lastname"),
            email,
            passwordEncoder.encode(request.password()));
    user.setDocumentType(documentType);
    user.setDocumentNumber(documentNumber);
    user.setBirthDate(request.birthDate());
    user.setPhoneNumber(normalizeOptional(request.phoneNumber()));
    user.setAddress(normalizeOptional(request.address()));
    user = userRepository.save(user);

    for (Role role : roles) {
      UserRole assignment = new UserRole(user, role);
      user.getUserRoles().add(assignment);
      userRoleRepository.save(assignment);
    }

    StudentProfile profile = null;
    if (isStudent) {
      profile =
          studentProfileRepository.save(
              new StudentProfile(
                  user,
                  requireNonBlank(request.studentProfile().studentCode(), "studentCode"),
                  requireNonBlank(request.studentProfile().major(), "major")));
    }

    return toUserResponse(user, requestedRoleNames, profile);
  }

  @Override
  public UserResponse findById(Integer id) {
    return toStoredUserResponse(requireUser(id));
  }

  @Override
  @Transactional
  public UserResponse update(Integer id, UserUpdateRequest request) {
    User user = requireUser(id);
    updateIdentity(user, request);

    Set<String> roleNames =
        request.roles() == null
            ? currentRoleNames(id)
            : updateRoleAssignments(user, normalizeRoles(request.roles()));

    if (request.active() != null) {
      user.setActive(request.active());
      if (!request.active()) {
        revokeRefreshTokens(id);
      }
    }

    user.setUpdatedAt(clock.instant());
    StudentProfile profile = studentProfileRepository.findByUserId(id).orElse(null);
    return toUserResponse(user, roleNames, profile);
  }

  @Override
  @Transactional
  public void delete(Integer id) {
    User user = requireUser(id);
    user.setActive(false);
    user.setUpdatedAt(clock.instant());
    revokeRefreshTokens(id);
  }

  private void updateIdentity(User user, UserUpdateRequest request) {
    if (request.name() != null) {
      user.setName(requireNonBlank(request.name(), "name"));
    }
    if (request.lastname() != null) {
      user.setLastname(requireNonBlank(request.lastname(), "lastname"));
    }
    if (request.email() != null) {
      String email = normalizeEmail(request.email());
      if (!email.equals(user.getEmail()) && userRepository.existsByEmail(email)) {
        throw new DuplicateEmailException();
      }
      user.setEmail(email);
    }

    String documentType =
        request.documentType() == null
            ? user.getDocumentType()
            : normalizeOptional(request.documentType());
    String documentNumber =
        request.documentNumber() == null
            ? user.getDocumentNumber()
            : normalizeOptional(request.documentNumber());
    if (!Objects.equals(documentType, user.getDocumentType())
        || !Objects.equals(documentNumber, user.getDocumentNumber())) {
      assertDocumentAvailable(documentType, documentNumber);
    }

    if (request.documentType() != null) {
      user.setDocumentType(documentType);
    }
    if (request.documentNumber() != null) {
      user.setDocumentNumber(documentNumber);
    }
    if (request.birthDate() != null) {
      user.setBirthDate(request.birthDate());
    }
    if (request.phoneNumber() != null) {
      user.setPhoneNumber(normalizeOptional(request.phoneNumber()));
    }
    if (request.address() != null) {
      user.setAddress(normalizeOptional(request.address()));
    }
  }

  private Set<String> updateRoleAssignments(User user, Set<String> requestedRoleNames) {
    List<Role> requestedRoles = findRoles(requestedRoleNames);
    if (requestedRoleNames.contains("student")
        && studentProfileRepository.findByUserId(user.getId()).isEmpty()) {
      throw new IllegalArgumentException("Student role requires an academic profile");
    }

    List<UserRole> currentAssignments = userRoleRepository.findAllByUser_Id(user.getId());
    Set<String> currentRoleNames =
        currentAssignments.stream()
            .map(UserRole::getRole)
            .map(Role::getName)
            .collect(Collectors.toSet());

    for (UserRole assignment : currentAssignments) {
      String roleName = assignment.getRole().getName();
      if (!requestedRoleNames.contains(roleName)) {
        user.getUserRoles().remove(assignment);
      }
    }

    for (Role role : requestedRoles) {
      if (!currentRoleNames.contains(role.getName())) {
        UserRole assignment = new UserRole(user, role);
        user.getUserRoles().add(assignment);
        userRoleRepository.save(assignment);
      }
    }
    return requestedRoleNames;
  }

  private List<Role> findRoles(Set<String> roleNames) {
    if (roleNames.isEmpty()) {
      return List.of();
    }
    List<Role> roles = roleRepository.findAllByNameIn(roleNames);
    Set<String> foundRoleNames = roles.stream().map(Role::getName).collect(Collectors.toSet());
    if (!foundRoleNames.equals(roleNames)) {
      throw new InvalidRoleException();
    }
    return roles;
  }

  private Set<String> normalizeRoles(Set<String> roleNames) {
    if (roleNames == null) {
      throw new InvalidRoleException();
    }
    Set<String> normalized = new LinkedHashSet<>();
    for (String roleName : roleNames) {
      if (roleName == null || roleName.isBlank()) {
        throw new InvalidRoleException();
      }
      normalized.add(roleName.trim().toLowerCase(Locale.ROOT));
    }
    return normalized;
  }

  private Set<String> currentRoleNames(Integer userId) {
    return userRoleRepository.findAllByUser_Id(userId).stream()
        .map(UserRole::getRole)
        .map(Role::getName)
        .collect(Collectors.toCollection(LinkedHashSet::new));
  }

  private void assertDocumentAvailable(String documentType, String documentNumber) {
    if (documentType != null
        && documentNumber != null
        && userRepository.existsByDocumentTypeAndDocumentNumber(documentType, documentNumber)) {
      throw new DuplicateDocumentException();
    }
  }

  private void revokeRefreshTokens(Integer userId) {
    List<RefreshToken> activeTokens =
        refreshTokenRepository.findAllByUser_IdAndRevokedFalse(userId);
    activeTokens.forEach(token -> token.setRevoked(true));
  }

  private User requireUser(Integer id) {
    return userRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("User"));
  }

  private UserResponse toStoredUserResponse(User user) {
    return toUserResponse(
        user,
        currentRoleNames(user.getId()),
        studentProfileRepository.findByUserId(user.getId()).orElse(null));
  }

  private UserResponse toUserResponse(User user, Set<String> roleNames, StudentProfile profile) {
    StudentProfileResponse profileResponse =
        profile == null
            ? null
            : new StudentProfileResponse(
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
        Set.copyOf(roleNames),
        profileResponse,
        user.getCreatedAt(),
        user.getUpdatedAt());
  }

  private String normalizeEmail(String email) {
    if (email == null || email.isBlank()) {
      throw new IllegalArgumentException("Email must not be blank");
    }
    return email.trim().toLowerCase(Locale.ROOT);
  }

  private String normalizeOptional(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return value.trim();
  }

  private String requireNonBlank(String value, String field) {
    String normalized = normalizeOptional(value);
    if (normalized == null) {
      throw new IllegalArgumentException(field + " must not be blank");
    }
    return normalized;
  }
}
