package com.aireck.javapp.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "student_profiles")
public class StudentProfile {

  @Id
  @Column(name = "user_id")
  private Integer userId;

  @OneToOne(fetch = FetchType.LAZY, optional = false)
  @MapsId
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Column(name = "student_code", nullable = false, unique = true, length = 50)
  private String studentCode;

  @Column(name = "major", nullable = false, length = 100)
  private String major;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  protected StudentProfile() {}

  public StudentProfile(User user, String studentCode, String major) {
    this.user = user;
    this.studentCode = studentCode;
    this.major = major;
    this.createdAt = Instant.now();
    this.updatedAt = this.createdAt;
  }

  public Integer getUserId() {
    return userId;
  }

  public User getUser() {
    return user;
  }

  public String getStudentCode() {
    return studentCode;
  }

  public void setStudentCode(String studentCode) {
    this.studentCode = studentCode;
  }

  public String getMajor() {
    return major;
  }

  public void setMajor(String major) {
    this.major = major;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(Instant updatedAt) {
    this.updatedAt = updatedAt;
  }
}
