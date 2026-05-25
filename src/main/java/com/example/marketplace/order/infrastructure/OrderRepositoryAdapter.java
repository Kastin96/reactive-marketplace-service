package com.example.marketplace.order.infrastructure;

import com.example.marketplace.order.domain.Order;
import com.example.marketplace.order.domain.OrderItem;
import com.example.marketplace.order.domain.OrderRepository;
import com.example.marketplace.order.domain.OrderStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Repository
@RequiredArgsConstructor
class OrderRepositoryAdapter implements OrderRepository {

  private final OrderR2dbcRepository orderRepository;
  private final OrderItemR2dbcRepository orderItemRepository;
  private final R2dbcEntityTemplate entityTemplate;

  @Override
  public Mono<Order> save(Order order) {
    OrderEntity entity = toEntity(order);

    return orderRepository.existsById(order.getId())
        .flatMap(exists -> exists
            ? orderRepository.save(entity)
            : entityTemplate.insert(OrderEntity.class).using(entity)
        )
        .flatMap(saved -> orderItemRepository.deleteByOrderId(order.getId())
            .thenMany(Flux.fromIterable(order.getItems())
                .map(this::toEntity)
                .concatMap(item -> entityTemplate.insert(OrderItemEntity.class).using(item))
            )
            .then(Mono.just(saved))
        )
        .flatMap(this::withItems);
  }

  @Override
  public Mono<Order> findById(UUID id) {
    return orderRepository.findById(id)
        .flatMap(this::withItems);
  }

  @Override
  public Flux<Order> findByCustomerId(UUID customerId) {
    return orderRepository.findByCustomerId(customerId)
        .flatMap(this::withItems);
  }

  @Override
  public Flux<Order> findBySellerId(UUID sellerId) {
    return orderItemRepository.findBySellerId(sellerId)
        .map(OrderItemEntity::orderId)
        .distinct()
        .flatMap(this::findById);
  }

  @Override
  public Flux<Order> findAll() {
    return orderRepository.findAll()
        .flatMap(this::withItems);
  }

  @Override
  public Mono<Boolean> existsById(UUID id) {
    return orderRepository.existsById(id);
  }

  private Mono<Order> withItems(OrderEntity orderEntity) {
    return orderItemRepository.findByOrderId(orderEntity.id())
        .map(this::toDomain)
        .collectList()
        .map(items -> Order.restore(
            orderEntity.id(),
            orderEntity.customerId(),
            OrderStatus.valueOf(orderEntity.status()),
            orderEntity.totalAmount(),
            items,
            orderEntity.createdAt(),
            orderEntity.updatedAt()
        ));
  }

  private OrderEntity toEntity(Order order) {
    return new OrderEntity(
        order.getId(),
        order.getCustomerId(),
        order.getStatus().name(),
        order.getTotalAmount(),
        order.getCreatedAt(),
        order.getUpdatedAt()
    );
  }

  private OrderItemEntity toEntity(OrderItem item) {
    return new OrderItemEntity(
        item.id(),
        item.orderId(),
        item.productId(),
        item.sellerId(),
        item.productName(),
        item.unitPrice(),
        item.quantity(),
        item.lineTotal(),
        item.createdAt()
    );
  }

  private OrderItem toDomain(OrderItemEntity entity) {
    return new OrderItem(
        entity.id(),
        entity.orderId(),
        entity.productId(),
        entity.sellerId(),
        entity.productName(),
        entity.unitPrice(),
        entity.quantity(),
        entity.lineTotal(),
        entity.createdAt()
    );
  }
}
