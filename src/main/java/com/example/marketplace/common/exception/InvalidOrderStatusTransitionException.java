package com.example.marketplace.common.exception;

import com.example.marketplace.order.domain.OrderStatus;

public class InvalidOrderStatusTransitionException extends RuntimeException {

  public InvalidOrderStatusTransitionException(OrderStatus currentStatus, OrderStatus newStatus) {
    super("Invalid order status transition from " + currentStatus + " to " + newStatus);
  }
}
