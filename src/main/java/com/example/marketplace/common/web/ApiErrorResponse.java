package com.example.marketplace.common.web;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

@Schema(description = "Standard API error response.")
public record ApiErrorResponse(
    @Schema(description = "UTC timestamp when the error response was generated.", example = "2026-05-25T12:00:00Z")
    Instant timestamp,
    @Schema(description = "HTTP status code.", example = "400")
    int status,
    @Schema(description = "HTTP reason phrase.", example = "Bad Request")
    String error,
    @Schema(description = "Human-readable error message.", example = "Validation failed")
    String message,
    @Schema(description = "Request path that failed.", example = "/api/v1/customer/orders")
    String path,
    @Schema(description = "Validation field errors. Empty for non-validation failures.")
    List<FieldErrorResponse> fieldErrors
) {
}
