package com.example.marketplace.product.infrastructure;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

import java.util.UUID;

interface ProductR2dbcRepository extends ReactiveCrudRepository<ProductEntity, UUID> {

  Flux<ProductEntity> findByStatus(String status);

  Flux<ProductEntity> findBySellerId(UUID sellerId);
}
