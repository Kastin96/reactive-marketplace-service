package com.example.marketplace.auth.api;

import java.util.UUID;

import com.example.marketplace.user.domain.UserRole;
import com.example.marketplace.user.domain.UserStatus;

public record AuthUserResponse(
    UUID id,
    String email,
    UserRole role,
    UserStatus status
) {
}
