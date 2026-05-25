package com.example.marketplace.category.infrastructure;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

import java.util.UUID;

interface CategoryR2dbcRepository extends ReactiveCrudRepository<CategoryEntity, UUID> {

  @Query("SELECT EXISTS(SELECT 1 FROM categories WHERE LOWER(name) = LOWER(:name))")
  Mono<Boolean> existsByNameIgnoreCase(String name);
}
