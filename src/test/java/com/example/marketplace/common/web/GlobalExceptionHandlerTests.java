package com.example.marketplace.common.web;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTests {

  private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

  @Test
  void duplicateKeyExceptionReturnsConflictWithoutLeakingDatabaseDetails() {
    MockServerWebExchange exchange = MockServerWebExchange.from(
        MockServerHttpRequest.post("/api/v1/test")
    );

    ResponseEntity<ApiErrorResponse> response = handler.handleDuplicateKey(
        new DuplicateKeyException("duplicate key value violates unique constraint uk_users_email"),
        exchange
    );

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().status()).isEqualTo(409);
    assertThat(response.getBody().message()).isEqualTo("Resource already exists");
    assertThat(response.getBody().path()).isEqualTo("/api/v1/test");
  }
}
