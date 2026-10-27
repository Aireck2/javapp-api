package com.aireck.javapp.service.impl;

import com.aireck.javapp.dto.StudentProfileRequest;
import com.aireck.javapp.dto.StudentProfileResponse;
import com.aireck.javapp.model.StudentProfile;
import com.aireck.javapp.model.User;
import com.aireck.javapp.model.UserRole;
import com.aireck.javapp.repository.StudentProfileRepository;
import com.aireck.javapp.repository.UserRepository;
import com.aireck.javapp.repository.UserRoleRepository;
import com.aireck.javapp.service.StudentProfileService;
import com.aireck.javapp.service.exception.DuplicateStudentCodeException;
import com.aireck.javapp.service.exception.ForbiddenOperationException;
import com.aireck.javapp.service.exception.ResourceNotFoundException;
import java.time.Clock;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class StudentProfileServiceImpl implements StudentProfileService {

  private final UserRepository userRepository;
  private final UserRoleRepository userRoleRepository;
  private final StudentProfileRepository studentProfileRepository;
  private final Clock clock;

  @Autowired
  public StudentProfileServiceImpl(
      UserRepository userRepository,
      UserRoleRepository userRoleRepository,
      StudentProfileRepository studentProfileRepository) {
    this(userRepository, userRoleRepository, studentProfileRepository, Clock.systemUTC());
  }

  public StudentProfileServiceImpl(
      UserRepository userRepository,
      UserRoleRepository userRoleRepository,
      StudentProfileRepository studentProfileRepository,
      Clock clock) {
    this.userRepository = Objects.requireNonNull(userRepository);
    this.userRoleRepository = Objects.requireNonNull(userRoleRepository);
    this.studentProfileRepository = Objects.requireNonNull(studentProfileRepository);
    this.clock = Objects.requireNonNull(clock);
  }

  @Override
  public StudentProfileResponse findProfile(Integer userId, Integer actorId, boolean actorIsAdmin) {
    assertCanAccess(userId, actorId, actorIsAdmin);
    StudentProfile profile =
        studentProfileRepository
            .findByUserId(userId)
            .orElseThrow(() -> new ResourceNotFoundException("Student profile"));
    return toResponse(profile);
  }

  @Override
  @Transactional
  public StudentProfileResponse updateProfile(
      Integer userId, Integer actorId, boolean actorIsAdmin, StudentProfileRequest request) {
    assertCanAccess(userId, actorId, actorIsAdmin);
    User user =
        userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User"));
    String studentCode = requireNonBlank(request.studentCode(), "studentCode");
    String major = requireNonBlank(request.major(), "major");

    StudentProfile profile = studentProfileRepository.findByUserId(userId).orElse(null);
    if (profile == null) {
      boolean hasStudentRole =
          userRoleRepository.findAllByUser_Id(userId).stream()
              .map(UserRole::getRole)
              .anyMatch(role -> "student".equals(role.getName()));
      if (!hasStudentRole) {
        throw new IllegalArgumentException("Student profile requires the student role");
      }
      ensureStudentCodeAvailable(studentCode);
      profile = studentProfileRepository.save(new StudentProfile(user, studentCode, major));
    } else {
      if (!studentCode.equals(profile.getStudentCode())) {
        ensureStudentCodeAvailable(studentCode);
        profile.setStudentCode(studentCode);
      }
      profile.setMajor(major);
      profile.setUpdatedAt(clock.instant());
    }

    return toResponse(profile);
  }

  private void assertCanAccess(Integer userId, Integer actorId, boolean actorIsAdmin) {
    if (!actorIsAdmin && !Objects.equals(userId, actorId)) {
      throw new ForbiddenOperationException();
    }
  }

  private void ensureStudentCodeAvailable(String studentCode) {
    if (studentProfileRepository.existsByStudentCode(studentCode)) {
      throw new DuplicateStudentCodeException();
    }
  }

  private String requireNonBlank(String value, String field) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(field + " must not be blank");
    }
    return value.trim();
  }

  private StudentProfileResponse toResponse(StudentProfile profile) {
    return new StudentProfileResponse(
        profile.getUserId(),
        profile.getStudentCode(),
        profile.getMajor(),
        profile.getCreatedAt(),
        profile.getUpdatedAt());
  }
}
