package com.example.marketplace.common.exception;

import java.util.UUID;

public class UserBlockedException extends RuntimeException {

  public UserBlockedException(UUID userId) {
    super("User is blocked: " + userId);
  }
}
