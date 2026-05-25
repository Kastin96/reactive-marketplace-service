package com.example.marketplace.security;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

import javax.crypto.SecretKey;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;
import com.example.marketplace.config.JwtProperties;
import com.example.marketplace.user.domain.User;
import com.example.marketplace.user.domain.UserRole;

@Component
public class JwtTokenProvider {

  private static final String USER_ID_CLAIM = "user_id";
  private static final String EMAIL_CLAIM = "email";
  private static final String ROLE_CLAIM = "role";

  private final JwtProperties jwtProperties;
  private final SecretKey signingKey;

  public JwtTokenProvider(JwtProperties jwtProperties) {
    this.jwtProperties = jwtProperties;
    this.signingKey = Keys.hmacShaKeyFor(jwtProperties.secret().getBytes(StandardCharsets.UTF_8));
  }

  public String generateAccessToken(User user) {
    Instant now = Instant.now();
    Instant expiresAt = now.plus(jwtProperties.expiration());

    return Jwts.builder()
        .issuer(jwtProperties.issuer())
        .subject(user.getId().toString())
        .issuedAt(Date.from(now))
        .expiration(Date.from(expiresAt))
        .claim(USER_ID_CLAIM, user.getId().toString())
        .claim(EMAIL_CLAIM, user.getEmail())
        .claim(ROLE_CLAIM, user.getRole().name())
        .signWith(signingKey)
        .compact();
  }

  public boolean isValid(String token) {
    try {
      extractAuthenticationClaims(token);
      return true;
    } catch (JwtException | IllegalArgumentException exception) {
      return false;
    }
  }

  public JwtAuthenticationClaims extractAuthenticationClaims(String token) {
    Claims claims = parseClaims(token);

    return new JwtAuthenticationClaims(
        UUID.fromString(claims.get(USER_ID_CLAIM, String.class)),
        claims.get(EMAIL_CLAIM, String.class),
        UserRole.valueOf(claims.get(ROLE_CLAIM, String.class))
    );
  }

  public UUID extractUserId(String token) {
    return extractAuthenticationClaims(token).userId();
  }

  public String extractEmail(String token) {
    return extractAuthenticationClaims(token).email();
  }

  public UserRole extractRole(String token) {
    return extractAuthenticationClaims(token).role();
  }

  public long expiresInSeconds() {
    return jwtProperties.expiration().toSeconds();
  }

  private Claims parseClaims(String token) {
    return Jwts.parser()
        .verifyWith(signingKey)
        .requireIssuer(jwtProperties.issuer())
        .build()
        .parseSignedClaims(token)
        .getPayload();
  }
}
