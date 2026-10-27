package com.aireck.javapp.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@WebMvcTest(
    controllers = SecurityConfigTest.SecurityProbeController.class,
    properties = "app.security.jwt.secret=0123456789abcdef0123456789abcdef")
@Import({
  SecurityConfig.class,
  JwtTokenService.class,
  SecurityConfigTest.SecurityProbeController.class
})
class SecurityConfigTest {

  @Autowired private MockMvc mockMvc;

  @Test
  void publicRegistrationRouteNeedsNoBearerToken() throws Exception {
    mockMvc.perform(post("/api/v1/auth/register")).andExpect(status().isOk());
  }

  @Test
  void protectedUserRouteReturns401WithoutToken() throws Exception {
    mockMvc.perform(get("/api/v1/users")).andExpect(status().isUnauthorized());
  }

  @Test
  void studentCannotListUsers() throws Exception {
    mockMvc
        .perform(
            get("/api/v1/users")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_STUDENT"))))
        .andExpect(status().isForbidden());
  }

  @Test
  void adminCanListUsers() throws Exception {
    mockMvc
        .perform(
            get("/api/v1/users").with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
        .andExpect(status().isOk());
  }

  @Test
  void studentCanAccessStudentProfileRouteForOwnershipCheck() throws Exception {
    mockMvc
        .perform(
            get("/api/v1/users/17/student-profile")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_STUDENT"))))
        .andExpect(status().isOk());
  }

  @Test
  void professorCannotUseStudentOrAdminUserRoutes() throws Exception {
    mockMvc
        .perform(
            get("/api/v1/users/17/student-profile")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_PROFESSOR"))))
        .andExpect(status().isForbidden());
  }

  @Test
  void converterMapsRoleClaimsToSpringAuthorities() {
    Jwt jwt =
        Jwt.withTokenValue("test-token")
            .header("alg", "HS256")
            .subject("17")
            .claim("roles", List.of("admin", "student"))
            .issuedAt(Instant.parse("2025-01-01T00:00:00Z"))
            .expiresAt(Instant.parse("2025-01-01T00:15:00Z"))
            .build();

    AbstractAuthenticationToken authentication = new JwtAuthenticationConverter().convert(jwt);

    assertEquals(
        List.of("ROLE_ADMIN", "ROLE_STUDENT"),
        authentication.getAuthorities().stream().map(Object::toString).sorted().toList());
  }

  @RestController
  static class SecurityProbeController {

    @PostMapping("/api/v1/auth/register")
    String register() {
      return "registered";
    }

    @GetMapping("/api/v1/users")
    String listUsers() {
      return "users";
    }

    @GetMapping("/api/v1/users/{id}/student-profile")
    String studentProfile() {
      return "profile";
    }
  }
}
