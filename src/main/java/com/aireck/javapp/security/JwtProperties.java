package com.aireck.javapp.security;

import java.nio.charset.StandardCharsets;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.security.jwt")
public final class JwtProperties {

  private String secret;

  public void setSecret(String secret) {
    this.secret = secret;
  }

  SecretKey secretKey() {
    if (secret == null || secret.isBlank()) {
      throw new IllegalStateException("JWT_SECRET must be configured");
    }

    byte[] secretBytes = secret.getBytes(StandardCharsets.UTF_8);
    if (secretBytes.length < 32) {
      throw new IllegalStateException("JWT_SECRET must contain at least 256 bits");
    }

    return new SecretKeySpec(secretBytes, "HmacSHA256");
  }
}
