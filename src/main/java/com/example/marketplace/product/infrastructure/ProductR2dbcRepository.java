package com.example.marketplace.product.infrastructure;

import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.UUID;

interface ProductR2dbcRepository extends ReactiveCrudRepository<ProductEntity, UUID> {

  Flux<ProductEntity> findByStatus(String status);

  Flux<ProductEntity> findBySellerId(UUID sellerId);

  @Modifying
  @Query("""
      UPDATE products
      SET stock_quantity = stock_quantity - :quantity,
          updated_at = :updatedAt
      WHERE id = :productId
        AND status = 'ACTIVE'
        AND stock_quantity >= :quantity
      """)
  Mono<Integer> decreaseStockIfAvailable(UUID productId, int quantity, LocalDateTime updatedAt);
}
