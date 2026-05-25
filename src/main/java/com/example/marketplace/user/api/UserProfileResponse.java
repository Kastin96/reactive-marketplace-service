package com.example.marketplace.user.api;

import java.util.UUID;

import com.example.marketplace.user.domain.UserRole;
import com.example.marketplace.user.domain.UserStatus;

public record UserProfileResponse(
    UUID id,
    String email,
    UserRole role,
    UserStatus status
) {
}
