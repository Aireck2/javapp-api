package com.aireck.javapp.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

class JwtTokenServiceTest {

  private static final Instant NOW = Instant.parse("2025-01-01T00:00:00Z");
  private static final String SECRET = "test-secret-0123456789abcdef-012345";

  @Test
  void issuesHs256TokenWithSubjectRolesAndFifteenMinuteLifetime() {
    JwtTokenService service = service();

    String encoded = service.issueAccessToken("42", List.of("student", "admin"));
    Jwt token = service.decodeAccessToken(encoded);

    assertEquals("42", token.getSubject());
    assertEquals(List.of("student", "admin"), token.getClaimAsStringList("roles"));
    assertEquals(NOW, token.getIssuedAt());
    assertEquals(NOW.plus(Duration.ofMinutes(15)), token.getExpiresAt());
    assertEquals("HS256", token.getHeaders().get("alg"));
  }

  @Test
  void rejectsTokenWithInvalidSignature() {
    JwtTokenService service = service();
    String encoded = service.issueAccessToken("42", List.of("student"));
    String[] parts = encoded.split("\\.");
    char firstSignatureCharacter = parts[2].charAt(0);
    parts[2] = (firstSignatureCharacter == 'a' ? "b" : "a") + parts[2].substring(1);

    assertThrows(JwtException.class, () -> service.decodeAccessToken(String.join(".", parts)));
  }

  @Test
  void rejectsExpiredToken() {
    JwtProperties properties = properties();
    JwtTokenService service = new JwtTokenService(properties, Clock.fixed(NOW, ZoneOffset.UTC));
    NimbusJwtEncoder encoder =
        NimbusJwtEncoder.withSecretKey(properties.secretKey())
            .algorithm(MacAlgorithm.HS256)
            .build();
    String expiredToken =
        encoder
            .encode(
                JwtEncoderParameters.from(
                    JwsHeader.with(MacAlgorithm.HS256).build(),
                    JwtClaimsSet.builder()
                        .subject("42")
                        .issuedAt(NOW.minus(Duration.ofMinutes(20)))
                        .expiresAt(NOW.minus(Duration.ofMinutes(5)))
                        .claim("roles", List.of("student"))
                        .build()))
            .getTokenValue();

    assertThrows(JwtException.class, () -> service.decodeAccessToken(expiredToken));
  }

  private JwtTokenService service() {
    return new JwtTokenService(properties(), Clock.fixed(NOW, ZoneOffset.UTC));
  }

  private JwtProperties properties() {
    JwtProperties properties = new JwtProperties();
    properties.setSecret(SECRET);
    return properties;
  }
}
