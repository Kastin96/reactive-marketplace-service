package com.example.marketplace.order.infrastructure;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

import java.util.UUID;

interface OrderR2dbcRepository extends ReactiveCrudRepository<OrderEntity, UUID> {

  Flux<OrderEntity> findByCustomerId(UUID customerId);
}
