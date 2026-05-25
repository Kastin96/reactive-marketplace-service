package com.example.marketplace.security;

import java.util.UUID;

import com.example.marketplace.user.domain.UserRole;

public record AuthenticatedUser(
    UUID id,
    String email,
    UserRole role
) {
}
