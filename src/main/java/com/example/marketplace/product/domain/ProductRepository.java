package com.example.marketplace.product.domain;

import com.example.marketplace.common.pagination.PageRequest;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface ProductRepository {

  Mono<Product> save(Product product);

  Mono<Product> findById(UUID id);

  Flux<Product> findAllActive(PageRequest pageRequest);

  Mono<Long> countAllActive();

  Flux<Product> findBySellerId(UUID sellerId, PageRequest pageRequest);

  Mono<Long> countBySellerId(UUID sellerId);

  Mono<Product> decreaseStockIfAvailable(UUID productId, int quantity);
}
