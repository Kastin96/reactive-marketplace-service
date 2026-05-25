package com.example.marketplace.user.api;

import com.example.marketplace.user.domain.UserRole;
import com.example.marketplace.user.domain.UserStatus;

import java.util.UUID;

public record UserProfileResponse(
    UUID id,
    String email,
    UserRole role,
    UserStatus status
) {
}
