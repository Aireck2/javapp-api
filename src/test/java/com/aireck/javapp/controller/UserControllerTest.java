package com.aireck.javapp.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.aireck.javapp.dto.StudentProfileResponse;
import com.aireck.javapp.dto.UserResponse;
import com.aireck.javapp.security.JwtTokenService;
import com.aireck.javapp.security.SecurityConfig;
import com.aireck.javapp.service.StudentProfileService;
import com.aireck.javapp.service.UserService;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

@WebMvcTest(
    controllers = UserController.class,
    properties = "app.security.jwt.secret=0123456789abcdef0123456789abcdef")
@Import({SecurityConfig.class, JwtTokenService.class, ApiExceptionHandler.class})
class UserControllerTest {

  @Autowired private MockMvc mockMvc;
  @MockitoBean private UserService userService;
  @MockitoBean private StudentProfileService studentProfileService;

  @Test
  void onlyAdminCanListUsers() throws Exception {
    when(userService.findAll(any()))
        .thenReturn(new PageImpl<>(List.of(userResponse(17)), PageRequest.of(0, 20), 1));

    mockMvc.perform(get("/api/v1/users")).andExpect(status().isUnauthorized());
    mockMvc.perform(get("/api/v1/users").with(student())).andExpect(status().isForbidden());
    mockMvc
        .perform(get("/api/v1/users").with(admin()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].email").value("ana@example.test"))
        .andExpect(jsonPath("$.content[0].passwordHash").doesNotExist());
  }

  @Test
  void adminCanCreateUsers() throws Exception {
    when(userService.create(any())).thenReturn(userResponse(17));

    mockMvc
        .perform(
            post("/api/v1/users")
                .with(admin())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"name\":\"Ana\",\"lastname\":\"Perez\","
                        + "\"email\":\"ana@example.test\",\"password\":\"Password8\","
                        + "\"roles\":[\"student\"],"
                        + "\"studentProfile\":{\"studentCode\":\"S-104\","
                        + "\"major\":\"Computer Science\"}}"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.roles[0]").value("student"))
        .andExpect(jsonPath("$.passwordHash").doesNotExist());
  }

  @Test
  void ownerCanReadOwnUserButCannotReadAnotherUsersData() throws Exception {
    when(userService.findById(17)).thenReturn(userResponse(17));

    mockMvc
        .perform(get("/api/v1/users/17").with(student()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(17));
    mockMvc.perform(get("/api/v1/users/99").with(student())).andExpect(status().isForbidden());

    verify(userService).findById(17);
    verify(userService, never()).findById(99);
  }

  @Test
  void adminCanReadAnotherUsersData() throws Exception {
    when(userService.findById(99)).thenReturn(userResponse(99));

    mockMvc
        .perform(get("/api/v1/users/99").with(admin()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(99));
  }

  @Test
  void ownerCanUpdateOwnFieldsButCannotChangeRoleOrStatus() throws Exception {
    when(userService.update(eq(17), any())).thenReturn(userResponse(17));

    mockMvc
        .perform(
            put("/api/v1/users/17")
                .with(student())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Ana María\"}"))
        .andExpect(status().isOk());
    mockMvc
        .perform(
            put("/api/v1/users/17")
                .with(student())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"roles\":[\"admin\"],\"active\":true}"))
        .andExpect(status().isForbidden());
  }

  @Test
  void adminCanSoftDeleteUser() throws Exception {
    mockMvc.perform(delete("/api/v1/users/17").with(admin())).andExpect(status().isNoContent());

    verify(userService).delete(17);
  }

  @Test
  void studentCanReadOwnProfile() throws Exception {
    when(studentProfileService.findProfile(17, 17, false))
        .thenReturn(new StudentProfileResponse(17, "S-104", "Computer Science", null, null));

    mockMvc
        .perform(get("/api/v1/users/17/student-profile").with(student()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.studentCode").value("S-104"));
  }

  @Test
  void adminCanUpdateStudentProfile() throws Exception {
    when(studentProfileService.updateProfile(eq(17), eq(99), eq(true), any()))
        .thenReturn(new StudentProfileResponse(17, "S-105", "Biology", null, null));

    mockMvc
        .perform(
            put("/api/v1/users/17/student-profile")
                .with(admin())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"studentCode\":\"S-105\",\"major\":\"Biology\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.studentCode").value("S-105"));
  }

  private RequestPostProcessor student() {
    return jwt()
        .jwt(token -> token.subject("17"))
        .authorities(new SimpleGrantedAuthority("ROLE_STUDENT"));
  }

  private RequestPostProcessor admin() {
    return jwt()
        .jwt(token -> token.subject("99"))
        .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"));
  }

  private UserResponse userResponse(int id) {
    return new UserResponse(
        id,
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
        new StudentProfileResponse(id, "S-104", "Computer Science", Instant.EPOCH, Instant.EPOCH),
        Instant.EPOCH,
        Instant.EPOCH);
  }
}
