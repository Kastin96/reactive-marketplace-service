package com.example.marketplace.common.web;

import com.example.marketplace.common.exception.CategoryNotFoundException;
import com.example.marketplace.common.exception.DuplicateCategoryNameException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
class CategoryExceptionHandler {

  @ExceptionHandler(CategoryNotFoundException.class)
  @ResponseStatus(HttpStatus.NOT_FOUND)
  ErrorResponse handleCategoryNotFound(CategoryNotFoundException exception) {
    return new ErrorResponse(exception.getMessage());
  }

  @ExceptionHandler(DuplicateCategoryNameException.class)
  @ResponseStatus(HttpStatus.CONFLICT)
  ErrorResponse handleDuplicateCategoryName(DuplicateCategoryNameException exception) {
    return new ErrorResponse(exception.getMessage());
  }

  private record ErrorResponse(String message) {
  }
}
