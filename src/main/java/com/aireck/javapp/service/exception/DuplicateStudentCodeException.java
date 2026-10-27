package com.aireck.javapp.service.exception;

public class DuplicateStudentCodeException extends RuntimeException {

  public DuplicateStudentCodeException() {
    super("Student code is already registered");
  }
}
