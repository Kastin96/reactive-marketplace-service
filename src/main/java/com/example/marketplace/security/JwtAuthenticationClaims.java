package com.example.marketplace.security;

import java.util.UUID;

import com.example.marketplace.user.domain.UserRole;

public record JwtAuthenticationClaims(
    UUID userId,
    String email,
    UserRole role
) {
}
