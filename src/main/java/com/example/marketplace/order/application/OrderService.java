package com.example.marketplace.order.application;

import com.example.marketplace.common.exception.InactiveProductOrderException;
import com.example.marketplace.common.exception.InsufficientStockException;
import com.example.marketplace.common.exception.InvalidOrderStatusTransitionException;
import com.example.marketplace.common.exception.OrderAccessDeniedException;
import com.example.marketplace.common.exception.OrderNotFoundException;
import com.example.marketplace.common.exception.ProductNotFoundException;
import com.example.marketplace.order.api.CreateOrderItemRequest;
import com.example.marketplace.order.api.CreateOrderRequest;
import com.example.marketplace.order.api.OrderItemResponse;
import com.example.marketplace.order.api.OrderResponse;
import com.example.marketplace.order.api.UpdateOrderStatusRequest;
import com.example.marketplace.order.domain.Order;
import com.example.marketplace.order.domain.OrderItem;
import com.example.marketplace.order.domain.OrderRepository;
import com.example.marketplace.order.domain.OrderStatus;
import com.example.marketplace.product.domain.Product;
import com.example.marketplace.product.domain.ProductRepository;
import com.example.marketplace.product.domain.ProductStatus;
import com.example.marketplace.security.CurrentUserProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

  private final OrderRepository orderRepository;
  private final ProductRepository productRepository;
  private final CurrentUserProvider currentUserProvider;

  @Transactional
  public Mono<OrderResponse> createOrder(CreateOrderRequest request) {
    UUID orderId = UUID.randomUUID();

    return currentUserProvider.currentUserId()
        .flatMap(customerId -> Flux.fromIterable(request.items())
            .concatMap(itemRequest -> createItemAndDecreaseStock(orderId, itemRequest))
            .collectList()
            .map(items -> Order.createNew(customerId, items))
        )
        .flatMap(orderRepository::save)
        .doOnNext(order -> log.info(
            "Order created orderId={} userId={} status={}",
            order.getId(),
            order.getCustomerId(),
            order.getStatus()
        ))
        .map(this::toResponse);
  }

  public Flux<OrderResponse> getCurrentCustomerOrders() {
    return currentUserProvider.currentUserId()
        .flatMapMany(orderRepository::findByCustomerId)
        .map(this::toResponse);
  }

  public Mono<OrderResponse> getCurrentCustomerOrderById(UUID orderId) {
    return currentUserProvider.currentUserId()
        .flatMap(customerId -> findRequired(orderId)
            .flatMap(order -> requireCustomerOwner(order, customerId).thenReturn(order))
        )
        .map(this::toResponse);
  }

  public Flux<OrderResponse> getCurrentSellerOrders() {
    return currentUserProvider.currentUserId()
        .flatMapMany(sellerId -> orderRepository.findBySellerId(sellerId)
            .map(order -> toSellerResponse(order, sellerId))
        );
  }

  public Flux<OrderResponse> getAllOrders() {
    return orderRepository.findAll()
        .map(this::toResponse);
  }

  public Mono<OrderResponse> updateOrderStatus(UUID orderId, UpdateOrderStatusRequest request) {
    return findRequired(orderId)
        .map(order -> {
          try {
            order.changeStatus(request.status());
          } catch (InvalidOrderStatusTransitionException exception) {
            log.warn(
                "Invalid order status transition orderId={} status={} requestedStatus={}",
                order.getId(),
                order.getStatus(),
                request.status()
            );
            throw exception;
          }
          return order;
        })
        .flatMap(orderRepository::save)
        .doOnNext(order -> log.info("Order status updated orderId={} status={}", order.getId(), order.getStatus()))
        .map(this::toResponse);
  }

  public Mono<OrderResponse> cancelCurrentCustomerOrder(UUID orderId) {
    return currentUserProvider.currentUserId()
        .flatMap(customerId -> findRequired(orderId)
            .flatMap(order -> requireCustomerOwner(order, customerId).thenReturn(order))
        )
        .map(order -> {
          try {
            order.cancel();
          } catch (InvalidOrderStatusTransitionException exception) {
            log.warn(
                "Invalid order cancellation orderId={} status={} requestedStatus={}",
                order.getId(),
                order.getStatus(),
                OrderStatus.CANCELLED
            );
            throw exception;
          }
          return order;
        })
        .flatMap(orderRepository::save)
        .doOnNext(order -> log.info("Order cancelled orderId={} userId={} status={}",
            order.getId(),
            order.getCustomerId(),
            order.getStatus()
        ))
        .map(this::toResponse);
  }

  private Mono<OrderItem> createItemAndDecreaseStock(UUID orderId, CreateOrderItemRequest itemRequest) {
    return productRepository.findById(itemRequest.productId())
        .switchIfEmpty(Mono.error(() -> new ProductNotFoundException(itemRequest.productId())))
        .flatMap(product -> validateOrderableProduct(product, itemRequest.quantity()))
        .flatMap(product -> productRepository.decreaseStockIfAvailable(product.getId(), itemRequest.quantity())
            .switchIfEmpty(Mono.error(() -> new InsufficientStockException(product.getId())))
        )
        .map(product -> OrderItem.createNew(
            orderId,
            product.getId(),
            product.getSellerId(),
            product.getName(),
            product.getPrice(),
            itemRequest.quantity()
        ));
  }

  private Mono<Product> validateOrderableProduct(Product product, Integer quantity) {
    if (product.getStatus() != ProductStatus.ACTIVE) {
      log.warn("Inactive product order attempt productId={} status={}", product.getId(), product.getStatus());
      return Mono.error(new InactiveProductOrderException(product.getId()));
    }
    if (product.getStockQuantity() < quantity) {
      log.warn("Insufficient stock productId={}", product.getId());
      return Mono.error(new InsufficientStockException(product.getId()));
    }
    return Mono.just(product);
  }

  private Mono<Order> findRequired(UUID orderId) {
    return orderRepository.findById(orderId)
        .switchIfEmpty(Mono.error(() -> new OrderNotFoundException(orderId)));
  }

  private Mono<Void> requireCustomerOwner(Order order, UUID customerId) {
    if (!order.getCustomerId().equals(customerId)) {
      log.warn("Order access denied orderId={} userId={}", order.getId(), customerId);
      return Mono.error(new OrderAccessDeniedException(order.getId()));
    }
    return Mono.empty();
  }

  private OrderResponse toResponse(Order order) {
    return new OrderResponse(
        order.getId(),
        order.getCustomerId(),
        order.getStatus(),
        order.getTotalAmount(),
        order.getItems().stream().map(this::toItemResponse).toList(),
        order.getCreatedAt(),
        order.getUpdatedAt()
    );
  }

  private OrderResponse toSellerResponse(Order order, UUID sellerId) {
    List<OrderItemResponse> sellerItems = order.getItems().stream()
        .filter(item -> item.sellerId().equals(sellerId))
        .map(this::toItemResponse)
        .toList();
    BigDecimal sellerTotal = sellerItems.stream()
        .map(OrderItemResponse::lineTotal)
        .reduce(BigDecimal.ZERO, BigDecimal::add);

    return new OrderResponse(
        order.getId(),
        order.getCustomerId(),
        order.getStatus(),
        sellerTotal,
        sellerItems,
        order.getCreatedAt(),
        order.getUpdatedAt()
    );
  }

  private OrderItemResponse toItemResponse(OrderItem item) {
    return new OrderItemResponse(
        item.id(),
        item.productId(),
        item.sellerId(),
        item.productName(),
        item.unitPrice(),
        item.quantity(),
        item.lineTotal(),
        item.createdAt()
    );
  }
}
