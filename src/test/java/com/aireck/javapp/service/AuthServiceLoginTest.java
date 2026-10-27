package com.aireck.javapp.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.aireck.javapp.dto.LoginRequest;
import com.aireck.javapp.dto.RegisterRequest;
import com.aireck.javapp.dto.StudentProfileRequest;
import com.aireck.javapp.dto.TokenResponse;
import com.aireck.javapp.dto.UserResponse;
import com.aireck.javapp.model.RefreshToken;
import com.aireck.javapp.model.Role;
import com.aireck.javapp.model.User;
import com.aireck.javapp.model.UserRole;
import com.aireck.javapp.repository.RefreshTokenRepository;
import com.aireck.javapp.repository.RoleRepository;
import com.aireck.javapp.repository.StudentProfileRepository;
import com.aireck.javapp.repository.UserRepository;
import com.aireck.javapp.repository.UserRoleRepository;
import com.aireck.javapp.security.JwtTokenService;
import com.aireck.javapp.service.exception.DuplicateEmailException;
import com.aireck.javapp.service.exception.InvalidCredentialsException;
import com.aireck.javapp.service.impl.AuthServiceImpl;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AuthServiceLoginTest {

  private static final Instant NOW = Instant.parse("2025-02-03T10:15:30Z");

  @Mock private UserRepository userRepository;
  @Mock private RoleRepository roleRepository;
  @Mock private UserRoleRepository userRoleRepository;
  @Mock private StudentProfileRepository studentProfileRepository;
  @Mock private RefreshTokenRepository refreshTokenRepository;
  @Mock private PasswordEncoder passwordEncoder;
  @Mock private JwtTokenService jwtTokenService;

  @Test
  void registersOnlyStudentWithRequiredAcademicProfileAndEncodedPassword() {
    when(userRepository.existsByEmail("ana@example.test")).thenReturn(false);
    when(roleRepository.findByName("student")).thenReturn(Optional.of(new Role("student")));
    when(passwordEncoder.encode("Password8")).thenReturn("bcrypt-hash");
    when(userRepository.save(any(User.class)))
        .thenAnswer(
            invocation -> {
              User user = invocation.getArgument(0);
              ReflectionTestUtils.setField(user, "id", 17);
              return user;
            });
    when(studentProfileRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    UserResponse response =
        service()
            .register(
                new RegisterRequest(
                    "Ana",
                    "Pérez",
                    "ANA@EXAMPLE.TEST",
                    "Password8",
                    new StudentProfileRequest("S-104", "Computer Science")));

    assertEquals("ana@example.test", response.email());
    assertEquals(Set.of("student"), response.roles());
    assertEquals("S-104", response.studentProfile().studentCode());
    assertEquals("Computer Science", response.studentProfile().major());

    ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
    verify(userRepository).save(userCaptor.capture());
    assertEquals("bcrypt-hash", userCaptor.getValue().getPasswordHash());
    verify(passwordEncoder).encode("Password8");
    verify(roleRepository).findByName("student");
    verify(userRoleRepository).save(any(UserRole.class));
    verify(studentProfileRepository).save(any());
  }

  @Test
  void rejectsDuplicateRegistrationEmail() {
    when(userRepository.existsByEmail("ana@example.test")).thenReturn(true);

    assertThrows(
        DuplicateEmailException.class,
        () ->
            service()
                .register(
                    new RegisterRequest(
                        "Ana",
                        "Perez",
                        "ana@example.test",
                        "Password8",
                        new StudentProfileRequest("S-104", "Computer Science"))));
  }

  @Test
  void rejectsPasswordsOver72Utf8BytesBeforeHashing() {
    RegisterRequest request =
        new RegisterRequest(
            "Ana",
            "Perez",
            "ana@example.test",
            "é".repeat(37),
            new StudentProfileRequest("S-104", "Computer Science"));

    assertThrows(IllegalArgumentException.class, () -> service().register(request));
  }

  @Test
  void loginReturnsAccessAndRefreshTokensAndStoresOnlyRefreshHash() throws Exception {
    User user = new User("Ana", "Perez", "ana@example.test", "bcrypt-hash");
    ReflectionTestUtils.setField(user, "id", 17);
    UserRole studentRole = new UserRole(user, new Role("student"));
    when(userRepository.findByEmail("ana@example.test")).thenReturn(Optional.of(user));
    when(passwordEncoder.matches("Password8", "bcrypt-hash")).thenReturn(true);
    when(userRoleRepository.findAllByUser_Id(17)).thenReturn(List.of(studentRole));
    when(jwtTokenService.issueAccessToken("17", List.of("student"))).thenReturn("signed-jwt");
    when(refreshTokenRepository.save(any(RefreshToken.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    TokenResponse response = service().login(new LoginRequest("ana@example.test", "Password8"));

    assertEquals("signed-jwt", response.accessToken());
    assertEquals("Bearer", response.tokenType());
    assertEquals(Duration.ofMinutes(15).toSeconds(), response.expiresIn());
    assertTrue(response.refreshToken().length() >= 40);

    ArgumentCaptor<RefreshToken> tokenCaptor = ArgumentCaptor.forClass(RefreshToken.class);
    verify(refreshTokenRepository).save(tokenCaptor.capture());
    String expectedHash =
        HexFormat.of()
            .formatHex(
                MessageDigest.getInstance("SHA-256")
                    .digest(response.refreshToken().getBytes(StandardCharsets.UTF_8)));
    assertEquals(expectedHash, tokenCaptor.getValue().getTokenHash());
    assertNotEquals(response.refreshToken(), tokenCaptor.getValue().getTokenHash());
    assertEquals(NOW.plus(Duration.ofDays(30)), tokenCaptor.getValue().getExpiresAt());
  }

  @Test
  void loginUsesOneGenericErrorForUnknownUsersAndWrongPasswords() {
    when(userRepository.findByEmail("missing@example.test")).thenReturn(Optional.empty());
    User existingUser = new User("Ana", "Perez", "ana@example.test", "bcrypt-hash");
    when(userRepository.findByEmail("ana@example.test")).thenReturn(Optional.of(existingUser));
    when(passwordEncoder.matches("WrongPassword8", "bcrypt-hash")).thenReturn(false);

    InvalidCredentialsException unknownUserError =
        assertThrows(
            InvalidCredentialsException.class,
            () -> service().login(new LoginRequest("missing@example.test", "Password8")));
    InvalidCredentialsException wrongPasswordError =
        assertThrows(
            InvalidCredentialsException.class,
            () -> service().login(new LoginRequest("ana@example.test", "WrongPassword8")));

    assertEquals(unknownUserError.getMessage(), wrongPasswordError.getMessage());
  }

  @Test
  void loginRejectsInactiveAccountWithGenericCredentialError() {
    User user = new User("Ana", "Perez", "ana@example.test", "bcrypt-hash");
    user.setActive(false);
    when(userRepository.findByEmail("ana@example.test")).thenReturn(Optional.of(user));

    assertThrows(
        InvalidCredentialsException.class,
        () -> service().login(new LoginRequest("ana@example.test", "Password8")));
  }

  private AuthService service() {
    return new AuthServiceImpl(
        userRepository,
        roleRepository,
        userRoleRepository,
        studentProfileRepository,
        refreshTokenRepository,
        passwordEncoder,
        jwtTokenService,
        Clock.fixed(NOW, ZoneOffset.UTC));
  }
}
