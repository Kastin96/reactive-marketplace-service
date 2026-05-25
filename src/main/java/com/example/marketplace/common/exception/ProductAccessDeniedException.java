package com.example.marketplace.common.exception;

import java.util.UUID;

public class ProductAccessDeniedException extends RuntimeException {

  public ProductAccessDeniedException(UUID productId) {
    super("Access denied for product: " + productId);
  }
}
