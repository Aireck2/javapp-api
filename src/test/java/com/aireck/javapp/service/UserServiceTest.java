package com.aireck.javapp.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.aireck.javapp.dto.StudentProfileRequest;
import com.aireck.javapp.dto.UserCreateRequest;
import com.aireck.javapp.dto.UserResponse;
import com.aireck.javapp.dto.UserUpdateRequest;
import com.aireck.javapp.model.RefreshToken;
import com.aireck.javapp.model.Role;
import com.aireck.javapp.model.StudentProfile;
import com.aireck.javapp.model.User;
import com.aireck.javapp.model.UserRole;
import com.aireck.javapp.repository.RefreshTokenRepository;
import com.aireck.javapp.repository.RoleRepository;
import com.aireck.javapp.repository.StudentProfileRepository;
import com.aireck.javapp.repository.UserRepository;
import com.aireck.javapp.repository.UserRoleRepository;
import com.aireck.javapp.service.exception.DuplicateDocumentException;
import com.aireck.javapp.service.exception.DuplicateEmailException;
import com.aireck.javapp.service.exception.InvalidRoleException;
import com.aireck.javapp.service.impl.UserServiceImpl;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

  private static final Instant NOW = Instant.parse("2025-04-05T11:12:13Z");

  @Mock private UserRepository userRepository;
  @Mock private RoleRepository roleRepository;
  @Mock private UserRoleRepository userRoleRepository;
  @Mock private StudentProfileRepository studentProfileRepository;
  @Mock private RefreshTokenRepository refreshTokenRepository;
  @Mock private PasswordEncoder passwordEncoder;

  @Test
  void adminCreateAssignsExistingRolesAndCreatesStudentProfile() {
    UserCreateRequest request =
        createRequest(
            "student",
            Set.of("student", "admin"),
            new StudentProfileRequest("S-207", "Mathematics"));
    when(userRepository.existsByEmail("student@example.test")).thenReturn(false);
    when(userRepository.existsByDocumentTypeAndDocumentNumber("DNI", "70010020")).thenReturn(false);
    when(roleRepository.findAllByNameIn(Set.of("admin", "student")))
        .thenReturn(List.of(new Role("admin"), new Role("student")));
    when(passwordEncoder.encode("Password8")).thenReturn("bcrypt-hash");
    when(userRepository.save(any(User.class)))
        .thenAnswer(
            invocation -> {
              User user = invocation.getArgument(0);
              ReflectionTestUtils.setField(user, "id", 41);
              return user;
            });
    when(studentProfileRepository.save(any(StudentProfile.class)))
        .thenAnswer(
            invocation -> {
              StudentProfile profile = invocation.getArgument(0);
              ReflectionTestUtils.setField(profile, "userId", 41);
              return profile;
            });

    UserResponse response = service().create(request);

    assertEquals(41, response.id());
    assertEquals(Set.of("admin", "student"), response.roles());
    assertEquals("S-207", response.studentProfile().studentCode());
    assertEquals("DNI", response.documentType());
    assertEquals("70010020", response.documentNumber());
    assertTrue(response.active());
    verify(userRoleRepository, times(2)).save(any(UserRole.class));
    verify(studentProfileRepository).save(any(StudentProfile.class));
  }

  @Test
  void rejectsDuplicateEmailWhenAdminCreatesUser() {
    UserCreateRequest request = createRequest("student", Set.of("student"), null);
    when(userRepository.existsByEmail("student@example.test")).thenReturn(true);

    assertThrows(DuplicateEmailException.class, () -> service().create(request));
  }

  @Test
  void refusesToCreateStudentWithoutAcademicProfile() {
    UserCreateRequest request = createRequest("student", Set.of("student"), null);
    when(userRepository.existsByEmail("student@example.test")).thenReturn(false);
    when(userRepository.existsByDocumentTypeAndDocumentNumber("DNI", "70010020")).thenReturn(false);
    when(roleRepository.findAllByNameIn(Set.of("student")))
        .thenReturn(List.of(new Role("student")));

    assertThrows(IllegalArgumentException.class, () -> service().create(request));
  }

  @Test
  void refusesUnknownRolesInsteadOfSilentlyDroppingThem() {
    UserCreateRequest request = createRequest("professor", Set.of("student", "superuser"), null);
    when(userRepository.existsByEmail("professor@example.test")).thenReturn(false);
    when(userRepository.existsByDocumentTypeAndDocumentNumber("DNI", "70010020")).thenReturn(false);
    when(roleRepository.findAllByNameIn(Set.of("student", "superuser")))
        .thenReturn(List.of(new Role("student")));

    assertThrows(InvalidRoleException.class, () -> service().create(request));
  }

  @Test
  void rejectsDuplicateDocumentOnUpdate() {
    User user = userWithId(41);
    user.setDocumentType("DNI");
    user.setDocumentNumber("70010020");
    when(userRepository.findById(41)).thenReturn(Optional.of(user));
    when(userRepository.existsByDocumentTypeAndDocumentNumber("DNI", "70010021")).thenReturn(true);

    assertThrows(
        DuplicateDocumentException.class,
        () ->
            service()
                .update(
                    41,
                    new UserUpdateRequest(
                        null, null, null, null, "70010021", null, null, null, null, null)));
  }

  @Test
  void rejectsDuplicateEmailOnUpdate() {
    User user = userWithId(41);
    when(userRepository.findById(41)).thenReturn(Optional.of(user));
    when(userRepository.existsByEmail("new@example.test")).thenReturn(true);

    assertThrows(
        DuplicateEmailException.class,
        () ->
            service()
                .update(
                    41,
                    new UserUpdateRequest(
                        null, null, "NEW@example.test", null, null, null, null, null, null, null)));
  }

  @Test
  void updateReplacesRoleAssignmentsWithRequestedExistingRoles() {
    User user = userWithId(41);
    UserRole currentAssignment = new UserRole(user, new Role("student"));
    user.getUserRoles().add(currentAssignment);
    when(userRepository.findById(41)).thenReturn(Optional.of(user));
    when(userRoleRepository.findAllByUser_Id(41)).thenReturn(List.of(currentAssignment));
    when(roleRepository.findAllByNameIn(Set.of("professor")))
        .thenReturn(List.of(new Role("professor")));
    when(studentProfileRepository.findByUserId(41)).thenReturn(Optional.empty());

    UserResponse response =
        service()
            .update(
                41,
                new UserUpdateRequest(
                    null, null, null, null, null, null, null, null, Set.of("professor"), null));

    assertEquals(Set.of("professor"), response.roles());
    assertFalse(user.getUserRoles().contains(currentAssignment));
    verify(userRoleRepository, never()).delete(any(UserRole.class));
    verify(userRoleRepository).save(any(UserRole.class));
  }

  @Test
  void listReturnsPagedPublicUserDataWithRolesAndProfile() {
    User user = userWithId(41);
    when(userRepository.findAll(PageRequest.of(0, 10)))
        .thenReturn(new PageImpl<>(List.of(user), PageRequest.of(0, 10), 1));
    when(userRoleRepository.findAllByUser_Id(41))
        .thenReturn(List.of(new UserRole(user, new Role("student"))));
    when(studentProfileRepository.findByUserId(41))
        .thenReturn(Optional.of(new StudentProfile(user, "S-207", "Mathematics")));

    var page = service().findAll(PageRequest.of(0, 10));

    assertEquals(1, page.getTotalElements());
    assertEquals(Set.of("student"), page.getContent().getFirst().roles());
    assertEquals("S-207", page.getContent().getFirst().studentProfile().studentCode());
  }

  @Test
  void deleteDeactivatesUserAndRevokesRefreshTokensWithoutDeletingRecord() {
    User user = userWithId(41);
    RefreshToken firstToken = new RefreshToken(user, "hash-one", null, NOW.plusSeconds(100));
    RefreshToken secondToken = new RefreshToken(user, "hash-two", null, NOW.plusSeconds(200));
    when(userRepository.findById(41)).thenReturn(Optional.of(user));
    when(refreshTokenRepository.findAllByUser_IdAndRevokedFalse(41))
        .thenReturn(List.of(firstToken, secondToken));

    service().delete(41);

    assertFalse(user.isActive());
    assertEquals(NOW, user.getUpdatedAt());
    assertTrue(firstToken.isRevoked());
    assertTrue(secondToken.isRevoked());
    verify(userRepository, never()).delete(any(User.class));
  }

  private UserService service() {
    return new UserServiceImpl(
        userRepository,
        roleRepository,
        userRoleRepository,
        studentProfileRepository,
        refreshTokenRepository,
        passwordEncoder,
        Clock.fixed(NOW, ZoneOffset.UTC));
  }

  private UserCreateRequest createRequest(
      String localEmail, Set<String> roles, StudentProfileRequest studentProfile) {
    return new UserCreateRequest(
        "Ana",
        "Perez",
        localEmail + "@example.test",
        "Password8",
        "DNI",
        "70010020",
        LocalDate.of(2000, 2, 3),
        "999123456",
        "Lima",
        roles,
        studentProfile);
  }

  private User userWithId(int id) {
    User user = new User("Ana", "Perez", "ana@example.test", "bcrypt-hash");
    ReflectionTestUtils.setField(user, "id", id);
    ReflectionTestUtils.setField(user, "createdAt", NOW.minusSeconds(100));
    ReflectionTestUtils.setField(user, "updatedAt", NOW.minusSeconds(100));
    return user;
  }
}
