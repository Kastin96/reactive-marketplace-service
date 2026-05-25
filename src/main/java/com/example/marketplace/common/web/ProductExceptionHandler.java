package com.example.marketplace.common.web;

import com.example.marketplace.common.exception.ProductAccessDeniedException;
import com.example.marketplace.common.exception.ProductNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
class ProductExceptionHandler {

  @ExceptionHandler(ProductNotFoundException.class)
  @ResponseStatus(HttpStatus.NOT_FOUND)
  ErrorResponse handleProductNotFound(ProductNotFoundException exception) {
    return new ErrorResponse(exception.getMessage());
  }

  @ExceptionHandler(ProductAccessDeniedException.class)
  @ResponseStatus(HttpStatus.FORBIDDEN)
  ErrorResponse handleProductAccessDenied(ProductAccessDeniedException exception) {
    return new ErrorResponse(exception.getMessage());
  }

  private record ErrorResponse(String message) {
  }
}
