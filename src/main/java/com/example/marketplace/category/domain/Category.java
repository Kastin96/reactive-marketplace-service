package com.example.marketplace.category.domain;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class Category {

  @NonNull
  private final UUID id;
  @NonNull
  private String name;
  private String description;
  @NonNull
  private final LocalDateTime createdAt;
  @NonNull
  private LocalDateTime updatedAt;

  public static Category createNew(String name, String description) {
    LocalDateTime now = LocalDateTime.now();
    return new Category(
        UUID.randomUUID(),
        name,
        description,
        now,
        now
    );
  }

  public static Category restore(
      UUID id,
      String name,
      String description,
      LocalDateTime createdAt,
      LocalDateTime updatedAt
  ) {
    return new Category(id, name, description, createdAt, updatedAt);
  }

  public void updateDetails(String name, String description) {
    this.name = name;
    this.description = description;
    this.updatedAt = LocalDateTime.now();
  }
}
