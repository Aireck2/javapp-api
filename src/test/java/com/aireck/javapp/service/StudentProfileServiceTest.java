package com.aireck.javapp.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import com.aireck.javapp.dto.StudentProfileRequest;
import com.aireck.javapp.dto.StudentProfileResponse;
import com.aireck.javapp.model.Role;
import com.aireck.javapp.model.StudentProfile;
import com.aireck.javapp.model.User;
import com.aireck.javapp.model.UserRole;
import com.aireck.javapp.repository.StudentProfileRepository;
import com.aireck.javapp.repository.UserRepository;
import com.aireck.javapp.repository.UserRoleRepository;
import com.aireck.javapp.service.exception.DuplicateStudentCodeException;
import com.aireck.javapp.service.exception.ForbiddenOperationException;
import com.aireck.javapp.service.impl.StudentProfileServiceImpl;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class StudentProfileServiceTest {

  private static final Instant NOW = Instant.parse("2025-05-06T07:08:09Z");

  @Mock private UserRepository userRepository;
  @Mock private UserRoleRepository userRoleRepository;
  @Mock private StudentProfileRepository studentProfileRepository;

  @Test
  void studentCanReadOwnProfile() {
    User user = userWithId(17);
    StudentProfile profile = new StudentProfile(user, "S-802", "Physics");
    ReflectionTestUtils.setField(profile, "userId", 17);
    when(studentProfileRepository.findByUserId(17)).thenReturn(Optional.of(profile));

    StudentProfileResponse response = service().findProfile(17, 17, false);

    assertEquals(17, response.userId());
    assertEquals("S-802", response.studentCode());
    assertEquals("Physics", response.major());
  }

  @Test
  void adminCanReadAnotherUsersProfile() {
    User user = userWithId(17);
    StudentProfile profile = new StudentProfile(user, "S-802", "Physics");
    ReflectionTestUtils.setField(profile, "userId", 17);
    when(studentProfileRepository.findByUserId(17)).thenReturn(Optional.of(profile));

    StudentProfileResponse response = service().findProfile(17, 99, true);

    assertEquals("S-802", response.studentCode());
  }

  @Test
  void nonAdminCannotReadAnotherUsersProfile() {
    assertThrows(ForbiddenOperationException.class, () -> service().findProfile(17, 99, false));
  }

  @Test
  void nonAdminCannotUpdateAnotherUsersProfile() {
    assertThrows(
        ForbiddenOperationException.class,
        () ->
            service()
                .updateProfile(17, 99, false, new StudentProfileRequest("S-804", "Chemistry")));
  }

  @Test
  void studentCanUpdateOwnProfile() {
    User user = userWithId(17);
    StudentProfile profile = new StudentProfile(user, "S-802", "Physics");
    ReflectionTestUtils.setField(profile, "userId", 17);
    when(userRepository.findById(17)).thenReturn(Optional.of(user));
    when(studentProfileRepository.findByUserId(17)).thenReturn(Optional.of(profile));
    when(studentProfileRepository.existsByStudentCode("S-803")).thenReturn(false);

    StudentProfileResponse response =
        service().updateProfile(17, 17, false, new StudentProfileRequest("S-803", "Astronomy"));

    assertEquals("S-803", response.studentCode());
    assertEquals("Astronomy", response.major());
    assertEquals(NOW, profile.getUpdatedAt());
  }

  @Test
  void refusesDuplicateStudentCode() {
    User user = userWithId(17);
    StudentProfile profile = new StudentProfile(user, "S-802", "Physics");
    ReflectionTestUtils.setField(profile, "userId", 17);
    when(userRepository.findById(17)).thenReturn(Optional.of(user));
    when(studentProfileRepository.findByUserId(17)).thenReturn(Optional.of(profile));
    when(studentProfileRepository.existsByStudentCode("S-999")).thenReturn(true);

    assertThrows(
        DuplicateStudentCodeException.class,
        () ->
            service()
                .updateProfile(17, 17, false, new StudentProfileRequest("S-999", "Astronomy")));
  }

  @Test
  void adminCanCreateProfileForExistingStudent() {
    User user = userWithId(17);
    when(userRepository.findById(17)).thenReturn(Optional.of(user));
    when(userRoleRepository.findAllByUser_Id(17))
        .thenReturn(List.of(new UserRole(user, new Role("student"))));
    when(studentProfileRepository.findByUserId(17)).thenReturn(Optional.empty());
    when(studentProfileRepository.existsByStudentCode("S-803")).thenReturn(false);
    when(studentProfileRepository.save(org.mockito.ArgumentMatchers.any(StudentProfile.class)))
        .thenAnswer(
            invocation -> {
              StudentProfile profile = invocation.getArgument(0);
              ReflectionTestUtils.setField(profile, "userId", 17);
              return profile;
            });

    StudentProfileResponse response =
        service().updateProfile(17, 99, true, new StudentProfileRequest("S-803", "Astronomy"));

    assertEquals(17, response.userId());
    assertEquals("S-803", response.studentCode());
    assertEquals("Astronomy", response.major());
  }

  private StudentProfileService service() {
    return new StudentProfileServiceImpl(
        userRepository,
        userRoleRepository,
        studentProfileRepository,
        Clock.fixed(NOW, ZoneOffset.UTC));
  }

  private User userWithId(int id) {
    User user = new User("Ana", "Perez", "ana@example.test", "bcrypt-hash");
    ReflectionTestUtils.setField(user, "id", id);
    return user;
  }
}
