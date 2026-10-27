package com.aireck.javapp.service.exception;

public class ResourceNotFoundException extends RuntimeException {

  public ResourceNotFoundException(String resourceName) {
    super(resourceName + " was not found");
  }
}
