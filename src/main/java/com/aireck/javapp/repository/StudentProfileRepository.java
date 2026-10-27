package com.aireck.javapp.repository;

import com.aireck.javapp.model.StudentProfile;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudentProfileRepository extends JpaRepository<StudentProfile, Integer> {

  Optional<StudentProfile> findByUserId(Integer userId);

  boolean existsByStudentCode(String studentCode);
}
