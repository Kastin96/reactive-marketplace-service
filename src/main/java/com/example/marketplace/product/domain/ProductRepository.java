package com.example.marketplace.product.domain;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface ProductRepository {

  Mono<Product> save(Product product);

  Mono<Product> findById(UUID id);

  Flux<Product> findAllActive();

  Flux<Product> findBySellerId(UUID sellerId);

  Mono<Product> decreaseStockIfAvailable(UUID productId, int quantity);
}
