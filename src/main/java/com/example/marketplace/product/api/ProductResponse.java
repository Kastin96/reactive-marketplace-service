package com.example.marketplace.product.api;

import com.example.marketplace.product.domain.ProductStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record ProductResponse(
    UUID id,
    UUID sellerId,
    UUID categoryId,
    String name,
    String description,
    BigDecimal price,
    Integer stockQuantity,
    ProductStatus status,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
}
