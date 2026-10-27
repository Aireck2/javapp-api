package com.aireck.javapp.service.exception;

public class DuplicateEmailException extends RuntimeException {

  public DuplicateEmailException() {
    super("Email is already registered");
  }
}
