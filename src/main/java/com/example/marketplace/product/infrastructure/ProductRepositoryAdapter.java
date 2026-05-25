package com.example.marketplace.product.infrastructure;

import com.example.marketplace.product.domain.Product;
import com.example.marketplace.product.domain.ProductRepository;
import com.example.marketplace.product.domain.ProductStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
class ProductRepositoryAdapter implements ProductRepository {

  private final ProductR2dbcRepository repository;
  private final R2dbcEntityTemplate entityTemplate;

  @Override
  public Mono<Product> save(Product product) {
    ProductEntity entity = toEntity(product);

    return repository.existsById(product.getId())
        .flatMap(exists -> exists
            ? repository.save(entity)
            : entityTemplate.insert(ProductEntity.class).using(entity)
        )
        .map(this::toDomain);
  }

  @Override
  public Mono<Product> findById(UUID id) {
    return repository.findById(id)
        .map(this::toDomain);
  }

  @Override
  public Flux<Product> findAllActive() {
    return repository.findByStatus(ProductStatus.ACTIVE.name())
        .map(this::toDomain);
  }

  @Override
  public Flux<Product> findBySellerId(UUID sellerId) {
    return repository.findBySellerId(sellerId)
        .map(this::toDomain);
  }

  @Override
  public Mono<Product> decreaseStockIfAvailable(UUID productId, int quantity) {
    if (quantity <= 0) {
      return Mono.error(new IllegalArgumentException("Quantity must be greater than zero"));
    }

    return repository.decreaseStockIfAvailable(productId, quantity, LocalDateTime.now())
        .flatMap(updatedRows -> updatedRows > 0 ? findById(productId) : Mono.empty());
  }

  private ProductEntity toEntity(Product product) {
    return new ProductEntity(
        product.getId(),
        product.getSellerId(),
        product.getCategoryId(),
        product.getName(),
        product.getDescription(),
        product.getPrice(),
        product.getStockQuantity(),
        product.getStatus().name(),
        product.getCreatedAt(),
        product.getUpdatedAt()
    );
  }

  private Product toDomain(ProductEntity entity) {
    return Product.restore(
        entity.id(),
        entity.sellerId(),
        entity.categoryId(),
        entity.name(),
        entity.description(),
        entity.price(),
        entity.stockQuantity(),
        ProductStatus.valueOf(entity.status()),
        entity.createdAt(),
        entity.updatedAt()
    );
  }
}
