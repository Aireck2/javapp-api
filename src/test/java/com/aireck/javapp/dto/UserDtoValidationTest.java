package com.aireck.javapp.dto;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import java.lang.reflect.Constructor;
import java.time.LocalDate;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class UserDtoValidationTest {

  @Test
  void adminCreateRequiresValidIdentityCredentialsAndAtLeastOneRole() {
    Set<String> invalidFields = validate(invalidUserCreateRequest());

    assertTrue(invalidFields.contains("name"));
    assertTrue(invalidFields.contains("lastname"));
    assertTrue(invalidFields.contains("email"));
    assertTrue(invalidFields.contains("password"));
    assertTrue(invalidFields.contains("roles"));
  }

  private Object invalidUserCreateRequest() {
    Class<?> profileType = dtoClass("StudentProfileRequest");
    Class<?> requestType = dtoClass("UserCreateRequest");
    Constructor<?> constructor =
        assertDoesNotThrow(
            () ->
                requestType.getConstructor(
                    String.class,
                    String.class,
                    String.class,
                    String.class,
                    String.class,
                    String.class,
                    LocalDate.class,
                    String.class,
                    String.class,
                    Set.class,
                    profileType));
    return assertDoesNotThrow(
        () ->
            constructor.newInstance(
                "", "", "not-an-email", "short", null, null, null, null, null, Set.of(), null));
  }

  private Set<String> validate(Object request) {
    try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
      return factory.getValidator().validate(request).stream()
          .map(violation -> violation.getPropertyPath().toString())
          .collect(Collectors.toSet());
    }
  }

  private Class<?> dtoClass(String simpleName) {
    return assertDoesNotThrow(
        () -> Class.forName("com.aireck.javapp.dto." + simpleName),
        "Expected request DTO " + simpleName + " to be available");
  }
}
