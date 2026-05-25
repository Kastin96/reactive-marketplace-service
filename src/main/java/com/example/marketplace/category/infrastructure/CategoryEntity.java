package com.example.marketplace.category.infrastructure;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;
import java.util.UUID;

@Table("categories")
record CategoryEntity(
    @Id
    UUID id,
    String name,
    String description,
    @Column("created_at")
    LocalDateTime createdAt,
    @Column("updated_at")
    LocalDateTime updatedAt
) {
}
