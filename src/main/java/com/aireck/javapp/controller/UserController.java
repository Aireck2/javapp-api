package com.aireck.javapp.controller;

import com.aireck.javapp.dto.StudentProfileRequest;
import com.aireck.javapp.dto.StudentProfileResponse;
import com.aireck.javapp.dto.UserCreateRequest;
import com.aireck.javapp.dto.UserResponse;
import com.aireck.javapp.dto.UserUpdateRequest;
import com.aireck.javapp.service.StudentProfileService;
import com.aireck.javapp.service.UserService;
import com.aireck.javapp.service.exception.ForbiddenOperationException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.Objects;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Users", description = "User management and student profile operations")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/v1/users")
public class UserController {

  private final UserService userService;
  private final StudentProfileService studentProfileService;

  public UserController(UserService userService, StudentProfileService studentProfileService) {
    this.userService = userService;
    this.studentProfileService = studentProfileService;
  }

  @Operation(
      summary = "List users",
      description = "Returns a paginated list of all users. Requires ADMIN role.")
  @ApiResponse(responseCode = "200", description = "Page of users")
  @ApiResponse(
      responseCode = "401",
      description = "Missing or invalid token",
      content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
  @ApiResponse(
      responseCode = "403",
      description = "Insufficient permissions",
      content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
  @GetMapping
  public Page<UserResponse> findAll(Pageable pageable) {
    return userService.findAll(pageable);
  }

  @Operation(
      summary = "Create user",
      description = "Creates a new user with the given roles. Requires ADMIN role.")
  @ApiResponse(responseCode = "201", description = "User created successfully")
  @ApiResponse(
      responseCode = "400",
      description = "Validation error",
      content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
  @ApiResponse(
      responseCode = "401",
      description = "Missing or invalid token",
      content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
  @ApiResponse(
      responseCode = "403",
      description = "Insufficient permissions",
      content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
  @ApiResponse(
      responseCode = "409",
      description = "Email or student code already in use",
      content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
  @PostMapping
  public ResponseEntity<UserResponse> create(@Valid @RequestBody UserCreateRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(userService.create(request));
  }

  @Operation(
      summary = "Get user by ID",
      description = "Returns a single user. Accessible by ADMIN or the user themselves.")
  @ApiResponse(responseCode = "200", description = "User found")
  @ApiResponse(
      responseCode = "401",
      description = "Missing or invalid token",
      content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
  @ApiResponse(
      responseCode = "403",
      description = "Insufficient permissions",
      content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
  @ApiResponse(
      responseCode = "404",
      description = "User not found",
      content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
  @GetMapping("/{id}")
  public UserResponse findById(
      @PathVariable Integer id, @AuthenticationPrincipal Jwt jwt, Authentication authentication) {
    assertOwnerOrAdmin(id, jwt, authentication);
    return userService.findById(id);
  }

  @Operation(
      summary = "Update user",
      description =
          "Updates a user's profile. ADMIN can change roles and active status; regular users can only update their own basic info.")
  @ApiResponse(responseCode = "200", description = "User updated successfully")
  @ApiResponse(
      responseCode = "400",
      description = "Validation error",
      content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
  @ApiResponse(
      responseCode = "401",
      description = "Missing or invalid token",
      content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
  @ApiResponse(
      responseCode = "403",
      description = "Insufficient permissions or attempted restricted field change",
      content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
  @ApiResponse(
      responseCode = "404",
      description = "User not found",
      content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
  @PutMapping("/{id}")
  public UserResponse update(
      @PathVariable Integer id,
      @Valid @RequestBody UserUpdateRequest request,
      @AuthenticationPrincipal Jwt jwt,
      Authentication authentication) {
    boolean admin = isAdmin(authentication);
    assertOwnerOrAdmin(id, jwt, authentication);
    if (!admin && (request.roles() != null || request.active() != null)) {
      throw new ForbiddenOperationException();
    }
    return userService.update(id, request);
  }

  @Operation(
      summary = "Delete user",
      description = "Permanently deletes a user. Requires ADMIN role.")
  @ApiResponse(responseCode = "204", description = "User deleted successfully")
  @ApiResponse(
      responseCode = "401",
      description = "Missing or invalid token",
      content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
  @ApiResponse(
      responseCode = "403",
      description = "Insufficient permissions",
      content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
  @ApiResponse(
      responseCode = "404",
      description = "User not found",
      content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable Integer id) {
    userService.delete(id);
    return ResponseEntity.noContent().build();
  }

  @Operation(
      summary = "Get student profile",
      description =
          "Returns the student profile for a given user. Accessible by ADMIN or the user themselves.")
  @ApiResponse(responseCode = "200", description = "Student profile found")
  @ApiResponse(
      responseCode = "401",
      description = "Missing or invalid token",
      content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
  @ApiResponse(
      responseCode = "403",
      description = "Insufficient permissions",
      content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
  @ApiResponse(
      responseCode = "404",
      description = "User or profile not found",
      content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
  @GetMapping("/{id}/student-profile")
  public StudentProfileResponse findStudentProfile(
      @PathVariable Integer id, @AuthenticationPrincipal Jwt jwt, Authentication authentication) {
    return studentProfileService.findProfile(id, principalId(jwt), isAdmin(authentication));
  }

  @Operation(
      summary = "Update student profile",
      description =
          "Creates or updates the student profile for a user. Accessible by ADMIN or the user themselves.")
  @ApiResponse(responseCode = "200", description = "Student profile updated successfully")
  @ApiResponse(
      responseCode = "400",
      description = "Validation error",
      content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
  @ApiResponse(
      responseCode = "401",
      description = "Missing or invalid token",
      content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
  @ApiResponse(
      responseCode = "403",
      description = "Insufficient permissions",
      content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
  @ApiResponse(
      responseCode = "404",
      description = "User not found",
      content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
  @PutMapping("/{id}/student-profile")
  public StudentProfileResponse updateStudentProfile(
      @PathVariable Integer id,
      @Valid @RequestBody StudentProfileRequest request,
      @AuthenticationPrincipal Jwt jwt,
      Authentication authentication) {
    return studentProfileService.updateProfile(
        id, principalId(jwt), isAdmin(authentication), request);
  }

  private void assertOwnerOrAdmin(Integer userId, Jwt jwt, Authentication authentication) {
    if (!isAdmin(authentication) && !Objects.equals(userId, principalId(jwt))) {
      throw new ForbiddenOperationException();
    }
  }

  private boolean isAdmin(Authentication authentication) {
    return authentication.getAuthorities().stream()
        .map(GrantedAuthority::getAuthority)
        .anyMatch("ROLE_ADMIN"::equals);
  }

  private Integer principalId(Jwt jwt) {
    try {
      return Integer.valueOf(jwt.getSubject());
    } catch (NumberFormatException | NullPointerException exception) {
      throw new ForbiddenOperationException();
    }
  }
}
