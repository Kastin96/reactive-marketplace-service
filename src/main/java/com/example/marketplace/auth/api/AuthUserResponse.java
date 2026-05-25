package com.example.marketplace.auth.api;

import com.example.marketplace.user.domain.UserRole;
import com.example.marketplace.user.domain.UserStatus;

import java.util.UUID;

public record AuthUserResponse(
    UUID id,
    String email,
    UserRole role,
    UserStatus status
) {
}
