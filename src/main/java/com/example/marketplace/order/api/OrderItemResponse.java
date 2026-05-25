package com.example.marketplace.order.api;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record OrderItemResponse(
    UUID id,
    UUID productId,
    UUID sellerId,
    String productName,
    BigDecimal unitPrice,
    Integer quantity,
    BigDecimal lineTotal,
    LocalDateTime createdAt
) {
}
