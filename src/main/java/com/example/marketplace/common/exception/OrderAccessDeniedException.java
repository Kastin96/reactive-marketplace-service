package com.example.marketplace.common.exception;

import java.util.UUID;

public class OrderAccessDeniedException extends RuntimeException {

  public OrderAccessDeniedException(UUID orderId) {
    super("Access denied for order: " + orderId);
  }
}
