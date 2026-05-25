package com.example.marketplace.common.exception;

import java.util.UUID;

public class InactiveProductOrderException extends RuntimeException {

  public InactiveProductOrderException(UUID productId) {
    super("Product is not active: " + productId);
  }
}
