package com.example.marketplace.order.infrastructure;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Table("order_items")
record OrderItemEntity(
    @Id
    UUID id,
    @Column("order_id")
    UUID orderId,
    @Column("product_id")
    UUID productId,
    @Column("seller_id")
    UUID sellerId,
    @Column("product_name")
    String productName,
    @Column("unit_price")
    BigDecimal unitPrice,
    Integer quantity,
    @Column("line_total")
    BigDecimal lineTotal,
    @Column("created_at")
    LocalDateTime createdAt
) {
}
