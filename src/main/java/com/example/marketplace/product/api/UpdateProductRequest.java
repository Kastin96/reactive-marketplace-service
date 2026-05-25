package com.example.marketplace.product.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

public record UpdateProductRequest(
    @NotNull UUID categoryId,
    @NotBlank @Size(max = 160) String name,
    @Size(max = 1000) String description,
    @NotNull @Positive BigDecimal price,
    @NotNull @PositiveOrZero Integer stockQuantity
) {
}
