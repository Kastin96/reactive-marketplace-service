package com.example.marketplace.order.api;

import com.example.marketplace.order.application.OrderService;
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
public class OrderController {

  private final OrderService orderService;

  @PostMapping("/api/v1/customer/orders")
  @ResponseStatus(HttpStatus.CREATED)
  public Mono<OrderResponse> createOrder(@Valid @RequestBody CreateOrderRequest request) {
    return orderService.createOrder(request);
  }

  @GetMapping("/api/v1/customer/orders")
  public Flux<OrderResponse> getCurrentCustomerOrders() {
    return orderService.getCurrentCustomerOrders();
  }

  @GetMapping("/api/v1/customer/orders/{orderId}")
  public Mono<OrderResponse> getCurrentCustomerOrderById(@PathVariable UUID orderId) {
    return orderService.getCurrentCustomerOrderById(orderId);
  }

  @PatchMapping("/api/v1/customer/orders/{orderId}/cancel")
  public Mono<OrderResponse> cancelCurrentCustomerOrder(@PathVariable UUID orderId) {
    return orderService.cancelCurrentCustomerOrder(orderId);
  }

  @GetMapping("/api/v1/seller/orders")
  public Flux<OrderResponse> getCurrentSellerOrders() {
    return orderService.getCurrentSellerOrders();
  }

  @GetMapping("/api/v1/admin/orders")
  public Flux<OrderResponse> getAllOrders() {
    return orderService.getAllOrders();
  }

  @PatchMapping("/api/v1/admin/orders/{orderId}/status")
  public Mono<OrderResponse> updateOrderStatus(
      @PathVariable UUID orderId,
      @Valid @RequestBody UpdateOrderStatusRequest request
  ) {
    return orderService.updateOrderStatus(orderId, request);
  }
}
