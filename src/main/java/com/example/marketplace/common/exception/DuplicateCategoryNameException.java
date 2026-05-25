package com.example.marketplace.common.exception;

public class DuplicateCategoryNameException extends RuntimeException {

  public DuplicateCategoryNameException(String name) {
    super("Category name already exists: " + name);
  }
}
