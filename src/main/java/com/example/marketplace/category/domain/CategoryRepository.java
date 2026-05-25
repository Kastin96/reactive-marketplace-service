package com.example.marketplace.category.domain;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface CategoryRepository {

  Mono<Category> save(Category category);

  Mono<Category> findById(UUID id);

  Flux<Category> findAll();

  Mono<Boolean> existsByNameIgnoreCase(String name);
}
