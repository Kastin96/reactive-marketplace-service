package com.example.marketplace.auth.api;

public record AuthResponse(
    String accessToken,
    String tokenType,
    long expiresIn,
    AuthUserResponse user
) {
}
