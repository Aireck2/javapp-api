package com.aireck.javapp.model;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.lang.reflect.Field;
import org.junit.jupiter.api.Test;

class EntityMappingTest {

  @Test
  void userMapsRequiredCredentialsToExistingUsersColumns() {
    Class<?> user = entityClass("User");

    assertTrue(user.isAnnotationPresent(Entity.class));
    assertEquals("users", user.getAnnotation(Table.class).name());

    Column email = column(user, "email");
    assertEquals("email", email.name());
    assertFalse(email.nullable());
    assertEquals(255, email.length());

    Column passwordHash = column(user, "passwordHash");
    assertEquals("password_hash", passwordHash.name());
    assertFalse(passwordHash.nullable());
    assertEquals("text", passwordHash.columnDefinition());
  }

  @Test
  void roleMapsUniqueNameToExistingRolesColumn() {
    Class<?> role = entityClass("Role");

    assertTrue(role.isAnnotationPresent(Entity.class));
    assertEquals("roles", role.getAnnotation(Table.class).name());

    Column name = column(role, "name");
    assertEquals("name", name.name());
    assertFalse(name.nullable());
    assertEquals(50, name.length());
    assertTrue(name.unique());
  }

  @Test
  void userMapsRoleAssignmentsThroughTheJoinEntity() {
    Field userRoles = mappedField(entityClass("User"), "userRoles");
    OneToMany relation = userRoles.getAnnotation(OneToMany.class);

    assertNotNull(relation, "Expected User.userRoles to map the join entity");
    assertEquals("user", relation.mappedBy());
  }

  @Test
  void studentProfileSharesUserPrimaryKeyAndMapsRequiredAcademicFields() {
    Class<?> profile = entityClass("StudentProfile");
    assertEquals("student_profiles", profile.getAnnotation(Table.class).name());

    Field user = mappedField(profile, "user");
    assertNotNull(user.getAnnotation(OneToOne.class));
    assertNotNull(user.getAnnotation(MapsId.class));

    Column studentCode = column(profile, "studentCode");
    assertEquals("student_code", studentCode.name());
    assertFalse(studentCode.nullable());
    assertEquals(50, studentCode.length());
    assertTrue(studentCode.unique());

    Column major = column(profile, "major");
    assertFalse(major.nullable());
    assertEquals(100, major.length());
  }

  @Test
  void refreshTokenMapsUniqueHashAndRequiredExpiry() {
    Class<?> refreshToken = entityClass("RefreshToken");
    assertEquals("refresh_tokens", refreshToken.getAnnotation(Table.class).name());

    Field user = mappedField(refreshToken, "user");
    assertNotNull(user.getAnnotation(ManyToOne.class));

    Column tokenHash = column(refreshToken, "tokenHash");
    assertEquals("token_hash", tokenHash.name());
    assertFalse(tokenHash.nullable());
    assertEquals(255, tokenHash.length());
    assertTrue(tokenHash.unique());

    Column expiresAt = column(refreshToken, "expiresAt");
    assertEquals("expires_at", expiresAt.name());
    assertFalse(expiresAt.nullable());
  }

  private Class<?> entityClass(String simpleName) {
    return assertDoesNotThrow(
        () -> Class.forName("com.aireck.javapp.model." + simpleName),
        "Expected JPA entity " + simpleName + " to be available");
  }

  private Column column(Class<?> entity, String fieldName) {
    Field field = mappedField(entity, fieldName);
    Column column = field.getAnnotation(Column.class);
    assertNotNull(column, "Expected @Column on " + entity.getSimpleName() + "." + fieldName);
    return column;
  }

  private Field mappedField(Class<?> entity, String fieldName) {
    return assertDoesNotThrow(
        () -> entity.getDeclaredField(fieldName),
        "Expected mapped field " + entity.getSimpleName() + "." + fieldName);
  }
}
