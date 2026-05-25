package com.example.marketplace.order.api;

import com.example.marketplace.order.domain.OrderStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateOrderStatusRequest(
    @NotNull OrderStatus status
) {
}
