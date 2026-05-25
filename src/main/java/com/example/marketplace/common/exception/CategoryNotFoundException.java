package com.example.marketplace.common.exception;

import java.util.UUID;

public class CategoryNotFoundException extends RuntimeException {

  public CategoryNotFoundException(UUID categoryId) {
    super("Category not found with id: " + categoryId);
  }
}
