package com.example.marketplace.product.infrastructure;

import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.UUID;

interface ProductR2dbcRepository extends ReactiveCrudRepository<ProductEntity, UUID> {

  @Query("""
      SELECT *
      FROM products
      WHERE status = :status
      ORDER BY created_at DESC, id DESC
      LIMIT :limit OFFSET :offset
      """)
  Flux<ProductEntity> findByStatus(String status, int limit, long offset);

  Mono<Long> countByStatus(String status);

  @Query("""
      SELECT *
      FROM products
      WHERE seller_id = :sellerId
      ORDER BY created_at DESC, id DESC
      LIMIT :limit OFFSET :offset
      """)
  Flux<ProductEntity> findBySellerId(UUID sellerId, int limit, long offset);

  Mono<Long> countBySellerId(UUID sellerId);

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
