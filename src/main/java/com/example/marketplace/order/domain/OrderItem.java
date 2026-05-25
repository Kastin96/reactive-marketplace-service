package com.example.marketplace.order.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record OrderItem(
    UUID id,
    UUID orderId,
    UUID productId,
    UUID sellerId,
    String productName,
    BigDecimal unitPrice,
    Integer quantity,
    BigDecimal lineTotal,
    LocalDateTime createdAt
) {

  public static OrderItem createNew(
      UUID orderId,
      UUID productId,
      UUID sellerId,
      String productName,
      BigDecimal unitPrice,
      Integer quantity
  ) {
    if (quantity == null || quantity <= 0) {
      throw new IllegalArgumentException("Order item quantity must be greater than zero");
    }

    return new OrderItem(
        UUID.randomUUID(),
        orderId,
        productId,
        sellerId,
        productName,
        unitPrice,
        quantity,
        unitPrice.multiply(BigDecimal.valueOf(quantity)),
        LocalDateTime.now()
    );
  }
}
