package com.example.marketplace.common.exception;

public class EmailAlreadyExistsException extends RuntimeException {

  public EmailAlreadyExistsException(String email) {
    super("Email already exists: " + email);
  }
}
