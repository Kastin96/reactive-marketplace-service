package com.example.marketplace.order.domain;

import com.example.marketplace.common.exception.InvalidOrderStatusTransitionException;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class Order {

  @NonNull
  private final UUID id;
  @NonNull
  private final UUID customerId;
  @NonNull
  private OrderStatus status;
  @NonNull
  private BigDecimal totalAmount;
  @NonNull
  private final List<OrderItem> items;
  @NonNull
  private final LocalDateTime createdAt;
  @NonNull
  private LocalDateTime updatedAt;

  public static Order createNew(UUID customerId, List<OrderItem> items) {
    LocalDateTime now = LocalDateTime.now();
    return new Order(
        items.getFirst().orderId(),
        customerId,
        OrderStatus.CREATED,
        calculateTotal(items),
        List.copyOf(items),
        now,
        now
    );
  }

  public static Order restore(
      UUID id,
      UUID customerId,
      OrderStatus status,
      BigDecimal totalAmount,
      List<OrderItem> items,
      LocalDateTime createdAt,
      LocalDateTime updatedAt
  ) {
    return new Order(id, customerId, status, totalAmount, List.copyOf(items), createdAt, updatedAt);
  }

  public void changeStatus(OrderStatus newStatus) {
    if (!canTransitionTo(newStatus)) {
      throw new InvalidOrderStatusTransitionException(status, newStatus);
    }

    this.status = newStatus;
    this.updatedAt = LocalDateTime.now();
  }

  public void cancel() {
    changeStatus(OrderStatus.CANCELLED);
  }

  private boolean canTransitionTo(OrderStatus newStatus) {
    return switch (status) {
      case CREATED -> newStatus == OrderStatus.PROCESSING || newStatus == OrderStatus.CANCELLED;
      case PROCESSING -> newStatus == OrderStatus.SHIPPED || newStatus == OrderStatus.CANCELLED;
      case SHIPPED -> newStatus == OrderStatus.DELIVERED;
      case DELIVERED, CANCELLED -> false;
    };
  }

  private static BigDecimal calculateTotal(List<OrderItem> items) {
    return items.stream()
        .map(OrderItem::lineTotal)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
  }
}
