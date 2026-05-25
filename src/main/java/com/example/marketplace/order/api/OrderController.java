package com.example.marketplace.order.api;

import com.example.marketplace.config.OpenApiConfig;
import com.example.marketplace.order.application.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Tag(name = "Orders", description = "Customer, seller, and admin order workflows.")
public class OrderController {

  private final OrderService orderService;

  @PostMapping("/api/v1/customer/orders")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
      summary = "Create order",
      description = "Requires CUSTOMER role. Product stock is reserved atomically.",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH_SCHEME)
  )
  public Mono<OrderResponse> createOrder(@Valid @RequestBody CreateOrderRequest request) {
    return orderService.createOrder(request);
  }

  @GetMapping("/api/v1/customer/orders")
  @Operation(
      summary = "List own orders",
      description = "Requires CUSTOMER role.",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH_SCHEME)
  )
  public Flux<OrderResponse> getCurrentCustomerOrders() {
    return orderService.getCurrentCustomerOrders();
  }

  @GetMapping("/api/v1/customer/orders/{orderId}")
  @Operation(
      summary = "Get own order",
      description = "Requires CUSTOMER role. Customers can access only their own orders.",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH_SCHEME)
  )
  public Mono<OrderResponse> getCurrentCustomerOrderById(@PathVariable UUID orderId) {
    return orderService.getCurrentCustomerOrderById(orderId);
  }

  @PatchMapping("/api/v1/customer/orders/{orderId}/cancel")
  @Operation(
      summary = "Cancel own order",
      description = "Requires CUSTOMER role. Only cancellable order statuses are accepted.",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH_SCHEME)
  )
  public Mono<OrderResponse> cancelCurrentCustomerOrder(@PathVariable UUID orderId) {
    return orderService.cancelCurrentCustomerOrder(orderId);
  }

  @GetMapping("/api/v1/seller/orders")
  @Operation(
      summary = "List seller orders",
      description = "Requires SELLER role. Response includes only order items that belong to the current seller.",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH_SCHEME)
  )
  public Flux<OrderResponse> getCurrentSellerOrders() {
    return orderService.getCurrentSellerOrders();
  }

  @GetMapping("/api/v1/admin/orders")
  @Operation(
      summary = "List all orders",
      description = "Requires ADMIN role.",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH_SCHEME)
  )
  public Flux<OrderResponse> getAllOrders() {
    return orderService.getAllOrders();
  }

  @PatchMapping("/api/v1/admin/orders/{orderId}/status")
  @Operation(
      summary = "Update order status",
      description = "Requires ADMIN role. Invalid status transitions are rejected.",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH_SCHEME)
  )
  public Mono<OrderResponse> updateOrderStatus(
      @PathVariable UUID orderId,
      @Valid @RequestBody UpdateOrderStatusRequest request
  ) {
    return orderService.updateOrderStatus(orderId, request);
  }
}
