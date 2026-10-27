package com.aireck.javapp.security;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.stereotype.Service;

@Service
public final class JwtTokenService {

  public static final Duration ACCESS_TOKEN_TTL = Duration.ofMinutes(15);

  private final JwtEncoder encoder;
  private final JwtDecoder decoder;
  private final Clock clock;

  @Autowired
  public JwtTokenService(JwtProperties properties) {
    this(properties, Clock.systemUTC());
  }

  JwtTokenService(JwtProperties properties, Clock clock) {
    Objects.requireNonNull(properties, "properties");
    this.clock = Objects.requireNonNull(clock, "clock");

    var secretKey = properties.secretKey();
    this.encoder = NimbusJwtEncoder.withSecretKey(secretKey).algorithm(MacAlgorithm.HS256).build();

    NimbusJwtDecoder jwtDecoder =
        NimbusJwtDecoder.withSecretKey(secretKey).macAlgorithm(MacAlgorithm.HS256).build();
    JwtTimestampValidator timestampValidator = new JwtTimestampValidator(Duration.ZERO);
    timestampValidator.setClock(clock);
    jwtDecoder.setJwtValidator(timestampValidator);
    this.decoder = jwtDecoder;
  }

  public String issueAccessToken(String subject, Collection<String> roles) {
    if (subject == null || subject.isBlank()) {
      throw new IllegalArgumentException("JWT subject must not be blank");
    }

    List<String> roleNames = List.copyOf(Objects.requireNonNull(roles, "roles"));
    Instant issuedAt = clock.instant();
    JwtClaimsSet claims =
        JwtClaimsSet.builder()
            .subject(subject)
            .issuedAt(issuedAt)
            .expiresAt(issuedAt.plus(ACCESS_TOKEN_TTL))
            .claim("roles", roleNames)
            .build();

    return encoder
        .encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
        .getTokenValue();
  }

  public Jwt decodeAccessToken(String encodedToken) {
    return decoder.decode(encodedToken);
  }

  public JwtDecoder jwtDecoder() {
    return decoder;
  }
}
