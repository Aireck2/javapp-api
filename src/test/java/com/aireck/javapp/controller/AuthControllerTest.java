package com.aireck.javapp.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.aireck.javapp.dto.StudentProfileResponse;
import com.aireck.javapp.dto.TokenResponse;
import com.aireck.javapp.dto.UserResponse;
import com.aireck.javapp.security.JwtTokenService;
import com.aireck.javapp.security.SecurityConfig;
import com.aireck.javapp.service.AuthService;
import java.time.Instant;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
    controllers = AuthController.class,
    properties = "app.security.jwt.secret=0123456789abcdef0123456789abcdef")
@Import({SecurityConfig.class, JwtTokenService.class, ApiExceptionHandler.class})
class AuthControllerTest {

  @Autowired private MockMvc mockMvc;
  @MockitoBean private AuthService authService;

  @Test
  void registerReturnsCreatedUserWithoutSensitiveFields() throws Exception {
    when(authService.register(any()))
        .thenReturn(
            new UserResponse(
                17,
                "Ana",
                "Perez",
                "ana@example.test",
                null,
                null,
                null,
                null,
                null,
                true,
                Set.of("student"),
                new StudentProfileResponse(
                    17, "S-104", "Computer Science", Instant.EPOCH, Instant.EPOCH),
                Instant.EPOCH,
                Instant.EPOCH));

    mockMvc
        .perform(
            post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"name\":\"Ana\",\"lastname\":\"Perez\","
                        + "\"email\":\"ana@example.test\",\"password\":\"Password8\","
                        + "\"studentProfile\":{\"studentCode\":\"S-104\","
                        + "\"major\":\"Computer Science\"}}"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.email").value("ana@example.test"))
        .andExpect(jsonPath("$.studentProfile.studentCode").value("S-104"))
        .andExpect(jsonPath("$.passwordHash").doesNotExist());

    verify(authService).register(any());
  }

  @Test
  void loginReturnsJsonAccessAndRefreshTokens() throws Exception {
    when(authService.login(any()))
        .thenReturn(new TokenResponse("access.jwt", "opaque-refresh", "Bearer", 900));

    mockMvc
        .perform(
            post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"ana@example.test\",\"password\":\"Password8\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.accessToken").value("access.jwt"))
        .andExpect(jsonPath("$.refreshToken").value("opaque-refresh"))
        .andExpect(jsonPath("$.tokenType").value("Bearer"))
        .andExpect(jsonPath("$.expiresIn").value(900));
  }

  @Test
  void refreshReturnsRotatedJsonTokenPair() throws Exception {
    when(authService.refresh(any()))
        .thenReturn(new TokenResponse("new.access.jwt", "new-refresh", "Bearer", 900));

    mockMvc
        .perform(
            post("/api/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\":\"old-refresh\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.accessToken").value("new.access.jwt"))
        .andExpect(jsonPath("$.refreshToken").value("new-refresh"));
  }

  @Test
  void logoutReturnsNoContent() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/auth/logout")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\":\"refresh-to-revoke\"}"))
        .andExpect(status().isNoContent());

    verify(authService).logout(any());
  }

  @Test
  void validatesRegistrationAndLoginPayloads() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("Request validation failed"));
    mockMvc
        .perform(
            post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"invalid\",\"password\":\"\"}"))
        .andExpect(status().isBadRequest());
  }
}
