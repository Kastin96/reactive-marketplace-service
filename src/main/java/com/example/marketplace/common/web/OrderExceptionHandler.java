package com.example.marketplace.common.web;

import com.example.marketplace.common.exception.InactiveProductOrderException;
import com.example.marketplace.common.exception.InsufficientStockException;
import com.example.marketplace.common.exception.InvalidOrderStatusTransitionException;
import com.example.marketplace.common.exception.OrderAccessDeniedException;
import com.example.marketplace.common.exception.OrderNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
class OrderExceptionHandler {

  @ExceptionHandler(OrderNotFoundException.class)
  @ResponseStatus(HttpStatus.NOT_FOUND)
  ErrorResponse handleOrderNotFound(OrderNotFoundException exception) {
    return new ErrorResponse(exception.getMessage());
  }

  @ExceptionHandler(OrderAccessDeniedException.class)
  @ResponseStatus(HttpStatus.FORBIDDEN)
  ErrorResponse handleOrderAccessDenied(OrderAccessDeniedException exception) {
    return new ErrorResponse(exception.getMessage());
  }

  @ExceptionHandler({
      InvalidOrderStatusTransitionException.class,
      InsufficientStockException.class,
      InactiveProductOrderException.class
  })
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  ErrorResponse handleInvalidOrder(RuntimeException exception) {
    return new ErrorResponse(exception.getMessage());
  }

  private record ErrorResponse(String message) {
  }
}
