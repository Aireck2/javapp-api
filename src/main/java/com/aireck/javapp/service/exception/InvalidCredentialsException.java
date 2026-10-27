package com.aireck.javapp.service.exception;

public class InvalidCredentialsException extends RuntimeException {

  public InvalidCredentialsException() {
    super("Invalid email or password");
  }
}
