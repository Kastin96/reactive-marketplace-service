package com.example.marketplace.order.domain;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface OrderRepository {

  Mono<Order> save(Order order);

  Mono<Order> findById(UUID id);

  Flux<Order> findByCustomerId(UUID customerId);

  Flux<Order> findBySellerId(UUID sellerId);

  Flux<Order> findAll();

  Mono<Boolean> existsById(UUID id);
}
