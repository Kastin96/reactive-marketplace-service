package com.example.marketplace.order.domain;

import com.example.marketplace.common.pagination.PageRequest;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface OrderRepository {

  Mono<Order> save(Order order);

  Mono<Order> findById(UUID id);

  Flux<Order> findByCustomerId(UUID customerId, PageRequest pageRequest);

  Mono<Long> countByCustomerId(UUID customerId);

  Flux<Order> findBySellerId(UUID sellerId, PageRequest pageRequest);

  Mono<Long> countBySellerId(UUID sellerId);

  Flux<Order> findAll(PageRequest pageRequest);

  Mono<Long> countAll();
}
