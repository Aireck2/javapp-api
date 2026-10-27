package com.aireck.javapp.dto;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import java.lang.reflect.Constructor;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class AuthDtoValidationTest {

  @Test
  void loginRejectsMalformedEmailAndBlankPassword() {
    Object request =
        construct(
            "LoginRequest", new Class<?>[] {String.class, String.class}, "not-an-email", "  ");

    Set<String> invalidFields = validate(request);

    assertTrue(invalidFields.contains("email"));
    assertTrue(invalidFields.contains("password"));
  }

  @Test
  void registrationRequiresAStudentProfile() {
    Set<String> invalidFields = validate(registrationRequest(null));

    assertTrue(invalidFields.contains("studentProfile"));
  }

  @Test
  void registrationValidatesNestedStudentProfileFields() {
    Object studentProfile = studentProfileRequest("", "");
    Set<String> invalidFields = validate(registrationRequest(studentProfile));

    assertTrue(invalidFields.contains("studentProfile.studentCode"));
    assertTrue(invalidFields.contains("studentProfile.major"));
  }

  @Test
  void refreshRejectsBlankToken() {
    Object request = construct("RefreshTokenRequest", new Class<?>[] {String.class}, "  ");

    assertTrue(validate(request).contains("refreshToken"));
  }

  private Set<String> validate(Object request) {
    try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
      return factory.getValidator().validate(request).stream()
          .map(violation -> violation.getPropertyPath().toString())
          .collect(Collectors.toSet());
    }
  }

  private Object construct(String simpleName, Class<?>[] parameterTypes, Object... arguments) {
    Class<?> requestType = dtoClass(simpleName);
    Constructor<?> constructor =
        assertDoesNotThrow(() -> requestType.getConstructor(parameterTypes));
    return assertDoesNotThrow(() -> constructor.newInstance(arguments));
  }

  private Object registrationRequest(Object studentProfile) {
    Class<?> profileType = dtoClass("StudentProfileRequest");
    Class<?> registerType = dtoClass("RegisterRequest");
    Constructor<?> constructor =
        assertDoesNotThrow(
            () ->
                registerType.getConstructor(
                    String.class, String.class, String.class, String.class, profileType));
    return assertDoesNotThrow(
        () ->
            constructor.newInstance(
                "Ana", "Perez", "ana@example.test", "Password8", studentProfile));
  }

  private Object studentProfileRequest(String studentCode, String major) {
    Class<?> profileType = dtoClass("StudentProfileRequest");
    Constructor<?> constructor =
        assertDoesNotThrow(() -> profileType.getConstructor(String.class, String.class));
    return assertDoesNotThrow(() -> constructor.newInstance(studentCode, major));
  }

  private Class<?> dtoClass(String simpleName) {
    return assertDoesNotThrow(
        () -> Class.forName("com.aireck.javapp.dto." + simpleName),
        "Expected request DTO " + simpleName + " to be available");
  }
}
