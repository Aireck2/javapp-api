package com.aireck.javapp.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.aireck.javapp.service.exception.DuplicateDocumentException;
import com.aireck.javapp.service.exception.DuplicateEmailException;
import com.aireck.javapp.service.exception.ForbiddenOperationException;
import com.aireck.javapp.service.exception.InvalidCredentialsException;
import com.aireck.javapp.service.exception.ResourceNotFoundException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@WebMvcTest(controllers = ApiExceptionHandlerTest.ExceptionProbeController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({ApiExceptionHandler.class, ApiExceptionHandlerTest.ExceptionProbeController.class})
class ApiExceptionHandlerTest {

  @Autowired private MockMvc mockMvc;

  @Test
  void invalidCredentialsReturnGeneric401WithoutDetails() throws Exception {
    mockMvc
        .perform(get("/probe/unauthorized"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.status").value(401))
        .andExpect(jsonPath("$.message").value("Invalid email or password"))
        .andExpect(jsonPath("$.trace").doesNotExist());
  }

  @Test
  void duplicatesReturn409() throws Exception {
    mockMvc
        .perform(get("/probe/duplicate-email"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.status").value(409));
    mockMvc
        .perform(get("/probe/duplicate-document"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.status").value(409));
  }

  @Test
  void missingAndForbiddenResourcesUse404And403() throws Exception {
    mockMvc
        .perform(get("/probe/not-found"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.status").value(404));
    mockMvc
        .perform(get("/probe/forbidden"))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.status").value(403));
  }

  @Test
  void invalidRequestReturns400() throws Exception {
    mockMvc
        .perform(
            post("/probe/validate")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400));
  }

  @Test
  void persistenceConflictReturns409WithoutDatabaseMessage() throws Exception {
    mockMvc
        .perform(get("/probe/persistence-conflict"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.message").value("Request conflicts with existing data"));
  }

  @Test
  void unexpectedErrorDoesNotExposeExceptionMessage() throws Exception {
    mockMvc
        .perform(get("/probe/internal-error"))
        .andExpect(status().isInternalServerError())
        .andExpect(jsonPath("$.message").value("Internal server error"));
  }

  @Test
  void unknownRouteRemains404() throws Exception {
    mockMvc
        .perform(get("/probe/unknown-route"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.status").value(404));
  }

  @RestController
  static class ExceptionProbeController {

    @GetMapping("/probe/unauthorized")
    String unauthorized() {
      throw new InvalidCredentialsException();
    }

    @GetMapping("/probe/duplicate-email")
    String duplicateEmail() {
      throw new DuplicateEmailException();
    }

    @GetMapping("/probe/duplicate-document")
    String duplicateDocument() {
      throw new DuplicateDocumentException();
    }

    @GetMapping("/probe/not-found")
    String notFound() {
      throw new ResourceNotFoundException("User");
    }

    @GetMapping("/probe/forbidden")
    String forbidden() {
      throw new ForbiddenOperationException();
    }

    @PostMapping("/probe/validate")
    String validate(@Valid @RequestBody ProbeRequest request) {
      return request.name();
    }

    @GetMapping("/probe/persistence-conflict")
    String persistenceConflict() {
      throw new DataIntegrityViolationException("sensitive constraint detail");
    }

    @GetMapping("/probe/internal-error")
    String internalError() {
      throw new IllegalStateException("sensitive internal detail");
    }
  }

  record ProbeRequest(@NotBlank String name) {}
}
