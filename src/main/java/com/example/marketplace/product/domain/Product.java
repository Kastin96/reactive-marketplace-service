package com.example.marketplace.product.domain;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class Product {

  @NonNull
  private final UUID id;
  @NonNull
  private final UUID sellerId;
  @NonNull
  private UUID categoryId;
  @NonNull
  private String name;
  private String description;
  @NonNull
  private BigDecimal price;
  @NonNull
  private Integer stockQuantity;
  @NonNull
  private ProductStatus status;
  @NonNull
  private final LocalDateTime createdAt;
  @NonNull
  private LocalDateTime updatedAt;

  public static Product createNew(
      UUID sellerId,
      UUID categoryId,
      String name,
      String description,
      BigDecimal price,
      Integer stockQuantity
  ) {
    validatePrice(price);
    validateStockQuantity(stockQuantity);

    LocalDateTime now = LocalDateTime.now();
    return new Product(
        UUID.randomUUID(),
        sellerId,
        categoryId,
        name,
        description,
        price,
        stockQuantity,
        ProductStatus.DRAFT,
        now,
        now
    );
  }

  public static Product restore(
      UUID id,
      UUID sellerId,
      UUID categoryId,
      String name,
      String description,
      BigDecimal price,
      Integer stockQuantity,
      ProductStatus status,
      LocalDateTime createdAt,
      LocalDateTime updatedAt
  ) {
    return new Product(id, sellerId, categoryId, name, description, price, stockQuantity, status, createdAt, updatedAt);
  }

  public void updateDetails(
      UUID categoryId,
      String name,
      String description,
      BigDecimal price,
      Integer stockQuantity
  ) {
    validatePrice(price);
    validateStockQuantity(stockQuantity);

    this.categoryId = categoryId;
    this.name = name;
    this.description = description;
    this.price = price;
    this.stockQuantity = stockQuantity;
    this.updatedAt = LocalDateTime.now();
  }

  public void activate() {
    this.status = ProductStatus.ACTIVE;
    this.updatedAt = LocalDateTime.now();
  }

  public void deactivate() {
    this.status = ProductStatus.INACTIVE;
    this.updatedAt = LocalDateTime.now();
  }

  public void decreaseStock(int quantity) {
    if (quantity <= 0) {
      throw new IllegalArgumentException("Quantity must be greater than zero");
    }
    if (stockQuantity < quantity) {
      throw new IllegalArgumentException("Stock quantity is not sufficient");
    }

    this.stockQuantity = stockQuantity - quantity;
    this.updatedAt = LocalDateTime.now();
  }

  private static void validatePrice(BigDecimal price) {
    if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
      throw new IllegalArgumentException("Product price must be greater than zero");
    }
  }

  private static void validateStockQuantity(Integer stockQuantity) {
    if (stockQuantity == null || stockQuantity < 0) {
      throw new IllegalArgumentException("Stock quantity must be zero or greater");
    }
  }
}
