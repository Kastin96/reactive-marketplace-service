package com.example.marketplace.order.infrastructure;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

interface OrderR2dbcRepository extends ReactiveCrudRepository<OrderEntity, UUID> {

  @Query("""
      SELECT *
      FROM orders
      WHERE customer_id = :customerId
      ORDER BY created_at DESC, id DESC
      LIMIT :limit OFFSET :offset
      """)
  Flux<OrderEntity> findByCustomerId(UUID customerId, int limit, long offset);

  Mono<Long> countByCustomerId(UUID customerId);

  @Query("""
      SELECT DISTINCT o.*
      FROM orders o
      JOIN order_items oi ON oi.order_id = o.id
      WHERE oi.seller_id = :sellerId
      ORDER BY o.created_at DESC, o.id DESC
      LIMIT :limit OFFSET :offset
      """)
  Flux<OrderEntity> findBySellerId(UUID sellerId, int limit, long offset);

  @Query("""
      SELECT COUNT(DISTINCT order_id)
      FROM order_items
      WHERE seller_id = :sellerId
      """)
  Mono<Long> countBySellerId(UUID sellerId);

  @Query("""
      SELECT *
      FROM orders
      ORDER BY created_at DESC, id DESC
      LIMIT :limit OFFSET :offset
      """)
  Flux<OrderEntity> findAll(int limit, long offset);
}
