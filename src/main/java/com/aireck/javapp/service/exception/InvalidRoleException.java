package com.aireck.javapp.service.exception;

public class InvalidRoleException extends IllegalArgumentException {

  public InvalidRoleException() {
    super("One or more requested roles do not exist");
  }
}
