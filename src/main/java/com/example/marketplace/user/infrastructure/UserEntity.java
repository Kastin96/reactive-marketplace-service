package com.example.marketplace.user.infrastructure;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("users")
public record UserEntity(
    @Id
    UUID id,
    String email,
    @Column("password_hash")
    String passwordHash,
    String role,
    String status,
    @Column("created_at")
    LocalDateTime createdAt,
    @Column("updated_at")
    LocalDateTime updatedAt
) {
}
