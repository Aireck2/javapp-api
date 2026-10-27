package com.aireck.javapp.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class JwtPropertiesTest {

  @Test
  void rejectsMissingSecret() {
    JwtProperties properties = new JwtProperties();

    assertThrows(IllegalStateException.class, properties::secretKey);
  }

  @Test
  void rejectsBlankSecret() {
    JwtProperties properties = new JwtProperties();
    properties.setSecret("  ");

    assertThrows(IllegalStateException.class, properties::secretKey);
  }

  @Test
  void rejectsSecretShorterThan256Bits() {
    JwtProperties properties = new JwtProperties();
    properties.setSecret("x".repeat(31));

    assertThrows(IllegalStateException.class, properties::secretKey);
  }

  @Test
  void createsHmacKeyFromAtLeast32Utf8Bytes() {
    String secret = "x".repeat(32);
    JwtProperties properties = new JwtProperties();
    properties.setSecret(secret);

    assertEquals(32, properties.secretKey().getEncoded().length);
    assertEquals("HmacSHA256", properties.secretKey().getAlgorithm());
  }
}
