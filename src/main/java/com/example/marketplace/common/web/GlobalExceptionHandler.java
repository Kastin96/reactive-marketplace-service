package com.example.marketplace.common.web;

import java.time.Instant;
import java.util.List;

import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.ServerWebInputException;
import com.example.marketplace.common.exception.CategoryNotFoundException;
import com.example.marketplace.common.exception.DuplicateCategoryNameException;
import com.example.marketplace.common.exception.EmailAlreadyExistsException;
import com.example.marketplace.common.exception.InactiveProductOrderException;
import com.example.marketplace.common.exception.InsufficientStockException;
import com.example.marketplace.common.exception.InvalidCredentialsException;
import com.example.marketplace.common.exception.InvalidOrderStatusTransitionException;
import com.example.marketplace.common.exception.OrderAccessDeniedException;
import com.example.marketplace.common.exception.OrderNotFoundException;
import com.example.marketplace.common.exception.ProductAccessDeniedException;
import com.example.marketplace.common.exception.ProductNotFoundException;
import com.example.marketplace.common.exception.UserBlockedException;
import com.example.marketplace.common.exception.UserNotFoundException;

@RestControllerAdvice
class GlobalExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  @ExceptionHandler(WebExchangeBindException.class)
  ResponseEntity<ApiErrorResponse> handleValidation(
      WebExchangeBindException exception,
      ServerWebExchange exchange
  ) {
    List<FieldErrorResponse> fieldErrors = exception.getBindingResult()
        .getFieldErrors()
        .stream()
        .map(this::toFieldError)
        .toList();

    return error(HttpStatus.BAD_REQUEST, "Validation failed", exchange, fieldErrors);
  }

  @ExceptionHandler({ConstraintViolationException.class, ServerWebInputException.class})
  ResponseEntity<ApiErrorResponse> handleInvalidRequest(RuntimeException exception, ServerWebExchange exchange) {
    return error(HttpStatus.BAD_REQUEST, "Invalid request", exchange);
  }

  @ExceptionHandler(InvalidCredentialsException.class)
  ResponseEntity<ApiErrorResponse> handleInvalidCredentials(
      InvalidCredentialsException exception,
      ServerWebExchange exchange
  ) {
    return error(HttpStatus.UNAUTHORIZED, exception.getMessage(), exchange);
  }

  @ExceptionHandler({
      CategoryNotFoundException.class,
      OrderNotFoundException.class,
      ProductNotFoundException.class,
      UserNotFoundException.class
  })
  ResponseEntity<ApiErrorResponse> handleNotFound(RuntimeException exception, ServerWebExchange exchange) {
    return error(HttpStatus.NOT_FOUND, exception.getMessage(), exchange);
  }

  @ExceptionHandler({
      DuplicateCategoryNameException.class,
      EmailAlreadyExistsException.class
  })
  ResponseEntity<ApiErrorResponse> handleConflict(RuntimeException exception, ServerWebExchange exchange) {
    return error(HttpStatus.CONFLICT, exception.getMessage(), exchange);
  }

  @ExceptionHandler({
      OrderAccessDeniedException.class,
      ProductAccessDeniedException.class,
      UserBlockedException.class
  })
  ResponseEntity<ApiErrorResponse> handleAccessDenied(RuntimeException exception, ServerWebExchange exchange) {
    return error(HttpStatus.FORBIDDEN, exception.getMessage(), exchange);
  }

  @ExceptionHandler({
      InactiveProductOrderException.class,
      InsufficientStockException.class,
      InvalidOrderStatusTransitionException.class,
      IllegalArgumentException.class
  })
  ResponseEntity<ApiErrorResponse> handleInvalidBusinessState(RuntimeException exception, ServerWebExchange exchange) {
    return error(HttpStatus.BAD_REQUEST, exception.getMessage(), exchange);
  }

  @ExceptionHandler(Throwable.class)
  ResponseEntity<ApiErrorResponse> handleUnexpected(Throwable exception, ServerWebExchange exchange) {
    log.error("Unexpected request failure", exception);
    return error(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected server error", exchange);
  }

  private FieldErrorResponse toFieldError(FieldError fieldError) {
    return new FieldErrorResponse(fieldError.getField(), fieldError.getDefaultMessage());
  }

  private ResponseEntity<ApiErrorResponse> error(
      HttpStatus status,
      String message,
      ServerWebExchange exchange
  ) {
    return error(status, message, exchange, List.of());
  }

  private ResponseEntity<ApiErrorResponse> error(
      HttpStatus status,
      String message,
      ServerWebExchange exchange,
      List<FieldErrorResponse> fieldErrors
  ) {
    return ResponseEntity.status(status)
        .body(new ApiErrorResponse(
            Instant.now(),
            status.value(),
            status.getReasonPhrase(),
            message,
            exchange.getRequest().getPath().value(),
            fieldErrors
        ));
  }
}
