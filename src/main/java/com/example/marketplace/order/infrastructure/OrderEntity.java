package com.example.marketplace.order.infrastructure;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Table("orders")
record OrderEntity(
    @Id
    UUID id,
    @Column("customer_id")
    UUID customerId,
    String status,
    @Column("total_amount")
    BigDecimal totalAmount,
    @Column("created_at")
    LocalDateTime createdAt,
    @Column("updated_at")
    LocalDateTime updatedAt
) {
}
