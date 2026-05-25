package com.example.marketplace.product.infrastructure;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Table("products")
record ProductEntity(
    @Id
    UUID id,
    @Column("seller_id")
    UUID sellerId,
    @Column("category_id")
    UUID categoryId,
    String name,
    String description,
    BigDecimal price,
    @Column("stock_quantity")
    Integer stockQuantity,
    String status,
    @Column("created_at")
    LocalDateTime createdAt,
    @Column("updated_at")
    LocalDateTime updatedAt
) {
}
