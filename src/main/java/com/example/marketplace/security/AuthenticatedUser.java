package com.example.marketplace.security;

import com.example.marketplace.user.domain.UserRole;

import java.util.UUID;

public record AuthenticatedUser(
    UUID id,
    String email,
    UserRole role
) {
}
