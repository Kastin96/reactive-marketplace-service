package com.example.marketplace.security;

import com.example.marketplace.user.domain.UserRole;

import java.util.UUID;

public record JwtAuthenticationClaims(
    UUID userId,
    String email,
    UserRole role
) {
}
