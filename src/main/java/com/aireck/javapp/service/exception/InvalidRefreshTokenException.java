package com.aireck.javapp.service.exception;

public class InvalidRefreshTokenException extends RuntimeException {

  public InvalidRefreshTokenException() {
    super("Invalid refresh token");
  }
}
