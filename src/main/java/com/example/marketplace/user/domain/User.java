package com.example.marketplace.user.domain;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class User {

  @NonNull
  private final UUID id;
  @NonNull
  private final String email;
  @NonNull
  private final String passwordHash;
  @NonNull
  private final UserRole role;
  @NonNull
  private UserStatus status;
  @NonNull
  private final LocalDateTime createdAt;
  @NonNull
  private LocalDateTime updatedAt;

  public static User createNewActiveUser(String email, String passwordHash, UserRole role) {
    LocalDateTime now = LocalDateTime.now();
    return new User(
        UUID.randomUUID(),
        email,
        passwordHash,
        role,
        UserStatus.ACTIVE,
        now,
        now
    );
  }

  public static User restore(
      UUID id,
      String email,
      String passwordHash,
      UserRole role,
      UserStatus status,
      LocalDateTime createdAt,
      LocalDateTime updatedAt
  ) {
    return new User(id, email, passwordHash, role, status, createdAt, updatedAt);
  }

  public void block() {
    this.status = UserStatus.BLOCKED;
    this.updatedAt = LocalDateTime.now();
  }

  public void activate() {
    this.status = UserStatus.ACTIVE;
    this.updatedAt = LocalDateTime.now();
  }
}
