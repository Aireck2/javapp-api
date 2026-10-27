package com.aireck.javapp.controller;

import com.aireck.javapp.dto.LoginRequest;
import com.aireck.javapp.dto.RefreshTokenRequest;
import com.aireck.javapp.dto.RegisterRequest;
import com.aireck.javapp.dto.TokenResponse;
import com.aireck.javapp.dto.UserResponse;
import com.aireck.javapp.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Auth", description = "Registration, login, token refresh and logout")
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

  private final AuthService authService;

  public AuthController(AuthService authService) {
    this.authService = authService;
  }

  @Operation(
      summary = "Register a new user",
      description =
          "Creates a user account together with a student profile. Returns the created user.")
  @ApiResponse(responseCode = "201", description = "User registered successfully")
  @ApiResponse(
      responseCode = "400",
      description = "Validation error",
      content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
  @ApiResponse(
      responseCode = "409",
      description = "Email or student code already in use",
      content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
  @PostMapping("/register")
  public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
  }

  @Operation(
      summary = "Login",
      description = "Authenticates credentials and returns an access + refresh token pair.")
  @ApiResponse(responseCode = "200", description = "Login successful")
  @ApiResponse(
      responseCode = "400",
      description = "Validation error",
      content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
  @ApiResponse(
      responseCode = "401",
      description = "Invalid credentials",
      content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
  @PostMapping("/login")
  public TokenResponse login(@Valid @RequestBody LoginRequest request) {
    return authService.login(request);
  }

  @Operation(
      summary = "Refresh access token",
      description = "Exchanges a valid refresh token for a new access + refresh token pair.")
  @ApiResponse(responseCode = "200", description = "Tokens refreshed successfully")
  @ApiResponse(
      responseCode = "400",
      description = "Validation error or invalid refresh token",
      content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
  @PostMapping("/refresh")
  public TokenResponse refresh(@Valid @RequestBody RefreshTokenRequest request) {
    return authService.refresh(request);
  }

  @Operation(summary = "Logout", description = "Invalidates the provided refresh token.")
  @ApiResponse(responseCode = "204", description = "Logged out successfully")
  @ApiResponse(
      responseCode = "400",
      description = "Validation error",
      content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
  @PostMapping("/logout")
  public ResponseEntity<Void> logout(@Valid @RequestBody RefreshTokenRequest request) {
    authService.logout(request);
    return ResponseEntity.noContent().build();
  }
}
