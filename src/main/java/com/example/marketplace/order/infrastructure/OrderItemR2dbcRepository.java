package com.example.marketplace.order.infrastructure;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

interface OrderItemR2dbcRepository extends ReactiveCrudRepository<OrderItemEntity, UUID> {

  Flux<OrderItemEntity> findByOrderId(UUID orderId);

  Mono<Void> deleteByOrderId(UUID orderId);
}
