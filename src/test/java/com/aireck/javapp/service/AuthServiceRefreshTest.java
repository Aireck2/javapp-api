package com.aireck.javapp.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.aireck.javapp.dto.RefreshTokenRequest;
import com.aireck.javapp.dto.TokenResponse;
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
import com.aireck.javapp.service.exception.InvalidRefreshTokenException;
import com.aireck.javapp.service.impl.AuthServiceImpl;
import jakarta.persistence.LockModeType;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

@ExtendWith(MockitoExtension.class)
class AuthServiceRefreshTest {

  private static final Instant NOW = Instant.parse("2025-02-03T10:15:30Z");
  private static final String PRESENTED_TOKEN = "presented-refresh-token-value";

  @Mock private UserRepository userRepository;
  @Mock private RoleRepository roleRepository;
  @Mock private UserRoleRepository userRoleRepository;
  @Mock private StudentProfileRepository studentProfileRepository;
  @Mock private RefreshTokenRepository refreshTokenRepository;
  @Mock private PasswordEncoder passwordEncoder;
  @Mock private JwtTokenService jwtTokenService;

  @Test
  void refreshRotatesTokenAndPersistsOnlyTheNewTokenHash() throws Exception {
    User user = user();
    RefreshToken oldToken = refreshToken(user, PRESENTED_TOKEN, NOW.plus(Duration.ofDays(1)));
    when(refreshTokenRepository.findForUpdateByTokenHash(hash(PRESENTED_TOKEN)))
        .thenReturn(Optional.of(oldToken));
    when(userRoleRepository.findAllByUser_Id(17))
        .thenReturn(List.of(new UserRole(user, new Role("student"))));
    when(jwtTokenService.issueAccessToken("17", List.of("student"))).thenReturn("new-access-jwt");
    when(refreshTokenRepository.save(any(RefreshToken.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    TokenResponse response = service().refresh(new RefreshTokenRequest(PRESENTED_TOKEN));

    assertTrue(oldToken.isRevoked());
    assertEquals("new-access-jwt", response.accessToken());
    assertNotEquals(PRESENTED_TOKEN, response.refreshToken());
    assertEquals(Duration.ofMinutes(15).toSeconds(), response.expiresIn());

    ArgumentCaptor<RefreshToken> tokenCaptor = ArgumentCaptor.forClass(RefreshToken.class);
    verify(refreshTokenRepository).save(tokenCaptor.capture());
    assertEquals(hash(response.refreshToken()), tokenCaptor.getValue().getTokenHash());
    assertNotEquals(response.refreshToken(), tokenCaptor.getValue().getTokenHash());
    assertEquals(NOW.plus(Duration.ofDays(30)), tokenCaptor.getValue().getExpiresAt());
  }

  @Test
  void rejectsExpiredRefreshToken() {
    User user = user();
    RefreshToken expired = refreshToken(user, PRESENTED_TOKEN, NOW.minusSeconds(1));
    when(refreshTokenRepository.findForUpdateByTokenHash(hash(PRESENTED_TOKEN)))
        .thenReturn(Optional.of(expired));

    assertThrows(
        InvalidRefreshTokenException.class,
        () -> service().refresh(new RefreshTokenRequest(PRESENTED_TOKEN)));
  }

  @Test
  void rejectsRevokedRefreshTokenReplay() {
    User user = user();
    RefreshToken replayed = refreshToken(user, PRESENTED_TOKEN, NOW.plus(Duration.ofDays(1)));
    replayed.setRevoked(true);
    when(refreshTokenRepository.findForUpdateByTokenHash(hash(PRESENTED_TOKEN)))
        .thenReturn(Optional.of(replayed));

    assertThrows(
        InvalidRefreshTokenException.class,
        () -> service().refresh(new RefreshTokenRequest(PRESENTED_TOKEN)));
  }

  @Test
  void rejectsRefreshTokenForInactiveAccount() {
    User user = user();
    user.setActive(false);
    RefreshToken token = refreshToken(user, PRESENTED_TOKEN, NOW.plus(Duration.ofDays(1)));
    when(refreshTokenRepository.findForUpdateByTokenHash(hash(PRESENTED_TOKEN)))
        .thenReturn(Optional.of(token));

    assertThrows(
        InvalidRefreshTokenException.class,
        () -> service().refresh(new RefreshTokenRequest(PRESENTED_TOKEN)));
  }

  @Test
  void rejectsUnknownRefreshToken() {
    when(refreshTokenRepository.findForUpdateByTokenHash(hash(PRESENTED_TOKEN)))
        .thenReturn(Optional.empty());

    assertThrows(
        InvalidRefreshTokenException.class,
        () -> service().refresh(new RefreshTokenRequest(PRESENTED_TOKEN)));
  }

  @Test
  void logoutRevokesPresentedRefreshToken() {
    User user = user();
    RefreshToken token = refreshToken(user, PRESENTED_TOKEN, NOW.plus(Duration.ofDays(1)));
    when(refreshTokenRepository.findForUpdateByTokenHash(hash(PRESENTED_TOKEN)))
        .thenReturn(Optional.of(token));

    service().logout(new RefreshTokenRequest(PRESENTED_TOKEN));

    assertTrue(token.isRevoked());
    verify(refreshTokenRepository).findForUpdateByTokenHash(hash(PRESENTED_TOKEN));
  }

  @Test
  void logoutIsIdempotentForUnknownRefreshToken() {
    when(refreshTokenRepository.findForUpdateByTokenHash(hash(PRESENTED_TOKEN)))
        .thenReturn(Optional.empty());

    service().logout(new RefreshTokenRequest(PRESENTED_TOKEN));
  }

  @Test
  void refreshUsesTransactionalServiceAndPessimisticWriteLookup() throws Exception {
    Method refreshMethod = AuthServiceImpl.class.getMethod("refresh", RefreshTokenRequest.class);
    Method logoutMethod = AuthServiceImpl.class.getMethod("logout", RefreshTokenRequest.class);
    Lock lock =
        RefreshTokenRepository.class
            .getMethod("findForUpdateByTokenHash", String.class)
            .getAnnotation(Lock.class);

    assertFalse(refreshMethod.getAnnotation(Transactional.class).readOnly());
    assertFalse(logoutMethod.getAnnotation(Transactional.class).readOnly());
    assertEquals(LockModeType.PESSIMISTIC_WRITE, lock.value());
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

  private User user() {
    User user = new User("Ana", "Perez", "ana@example.test", "bcrypt-hash");
    ReflectionTestUtils.setField(user, "id", 17);
    return user;
  }

  private RefreshToken refreshToken(User user, String rawToken, Instant expiresAt) {
    return new RefreshToken(user, hash(rawToken), null, expiresAt);
  }

  private String hash(String rawToken) {
    try {
      return HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256")
                  .digest(rawToken.getBytes(StandardCharsets.UTF_8)));
    } catch (Exception exception) {
      throw new AssertionError(exception);
    }
  }
}
