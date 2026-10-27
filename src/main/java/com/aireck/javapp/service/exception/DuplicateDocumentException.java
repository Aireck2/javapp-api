package com.aireck.javapp.service.exception;

public class DuplicateDocumentException extends RuntimeException {

  public DuplicateDocumentException() {
    super("Document is already registered");
  }
}
