package com.example.marketplace.common.web;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Validation error for a single request field.")
public record FieldErrorResponse(
    @Schema(description = "Field path from the request body.", example = "items[0].quantity")
    String field,
    @Schema(description = "Validation failure message.", example = "must be greater than or equal to 1")
    String message
) {
}
