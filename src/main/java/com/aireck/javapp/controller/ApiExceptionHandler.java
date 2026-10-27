package com.aireck.javapp.controller;

import com.aireck.javapp.service.exception.DuplicateDocumentException;
import com.aireck.javapp.service.exception.DuplicateEmailException;
import com.aireck.javapp.service.exception.DuplicateStudentCodeException;
import com.aireck.javapp.service.exception.ForbiddenOperationException;
import com.aireck.javapp.service.exception.InvalidCredentialsException;
import com.aireck.javapp.service.exception.InvalidRefreshTokenException;
import com.aireck.javapp.service.exception.ResourceNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.nio.file.AccessDeniedException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class ApiExceptionHandler {

  @ExceptionHandler({InvalidCredentialsException.class, InvalidRefreshTokenException.class})
  ResponseEntity<ApiErrorResponse> handleUnauthorized(
      RuntimeException exception, HttpServletRequest request) {
    return response(HttpStatus.UNAUTHORIZED, exception.getMessage(), request);
  }

  @ExceptionHandler({ForbiddenOperationException.class, AccessDeniedException.class})
  ResponseEntity<ApiErrorResponse> handleForbidden(
      RuntimeException exception, HttpServletRequest request) {
    return response(HttpStatus.FORBIDDEN, "Access is forbidden", request);
  }

  @ExceptionHandler(ResourceNotFoundException.class)
  ResponseEntity<ApiErrorResponse> handleNotFound(
      ResourceNotFoundException exception, HttpServletRequest request) {
    return response(HttpStatus.NOT_FOUND, exception.getMessage(), request);
  }

  @ExceptionHandler(NoResourceFoundException.class)
  ResponseEntity<ApiErrorResponse> handleNoResourceFound(HttpServletRequest request) {
    return response(HttpStatus.NOT_FOUND, "Resource was not found", request);
  }

  @ExceptionHandler({
    DuplicateEmailException.class,
    DuplicateDocumentException.class,
    DuplicateStudentCodeException.class,
    DataIntegrityViolationException.class
  })
  ResponseEntity<ApiErrorResponse> handleConflict(
      RuntimeException exception, HttpServletRequest request) {
    String message =
        exception instanceof DataIntegrityViolationException
            ? "Request conflicts with existing data"
            : exception.getMessage();
    return response(HttpStatus.CONFLICT, message, request);
  }

  @ExceptionHandler({
    MethodArgumentNotValidException.class,
    ConstraintViolationException.class,
    HttpMessageNotReadableException.class
  })
  ResponseEntity<ApiErrorResponse> handleInvalidRequest(
      Exception exception, HttpServletRequest request) {
    return response(HttpStatus.BAD_REQUEST, "Request validation failed", request);
  }

  @ExceptionHandler(IllegalArgumentException.class)
  ResponseEntity<ApiErrorResponse> handleBadRequest(
      IllegalArgumentException exception, HttpServletRequest request) {
    return response(HttpStatus.BAD_REQUEST, exception.getMessage(), request);
  }

  @ExceptionHandler(Exception.class)
  ResponseEntity<ApiErrorResponse> handleUnexpectedError(
      Exception exception, HttpServletRequest request) {
    return response(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error", request);
  }

  private ResponseEntity<ApiErrorResponse> response(
      HttpStatus status, String message, HttpServletRequest request) {
    return ResponseEntity.status(status)
        .body(
            new ApiErrorResponse(
                java.time.Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                request.getRequestURI()));
  }
}
