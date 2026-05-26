package com.example.marketplace.product.application;

import com.example.marketplace.category.domain.CategoryRepository;
import com.example.marketplace.common.exception.CategoryNotFoundException;
import com.example.marketplace.common.exception.ProductAccessDeniedException;
import com.example.marketplace.common.exception.ProductNotFoundException;
import com.example.marketplace.common.pagination.PageRequest;
import com.example.marketplace.common.pagination.PageResponse;
import com.example.marketplace.product.api.CreateProductRequest;
import com.example.marketplace.product.api.ProductResponse;
import com.example.marketplace.product.api.UpdateProductRequest;
import com.example.marketplace.product.domain.Product;
import com.example.marketplace.product.domain.ProductRepository;
import com.example.marketplace.product.domain.ProductStatus;
import com.example.marketplace.security.CurrentUserProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductService {

  private final ProductRepository productRepository;
  private final CategoryRepository categoryRepository;
  private final CurrentUserProvider currentUserProvider;

  public Mono<ProductResponse> createProduct(CreateProductRequest request) {
    String name = request.name().trim();

    return requireCategory(request.categoryId())
        .then(Mono.defer(currentUserProvider::currentUserId))
        .map(sellerId -> Product.createNew(
            sellerId,
            request.categoryId(),
            name,
            request.description(),
            request.price(),
            request.stockQuantity()
        ))
        .flatMap(productRepository::save)
        .doOnNext(product -> log.info(
            "Product created productId={} userId={} categoryId={} status={}",
            product.getId(),
            product.getSellerId(),
            product.getCategoryId(),
            product.getStatus()
        ))
        .map(this::toResponse);
  }

  public Mono<ProductResponse> updateOwnProduct(UUID productId, UpdateProductRequest request) {
    String name = request.name().trim();

    return requireCategory(request.categoryId())
        .then(Mono.defer(currentUserProvider::currentUserId))
        .flatMap(sellerId -> findRequired(productId)
            .flatMap(product -> requireOwner(product, sellerId)
                .then(Mono.fromSupplier(() -> {
                  product.updateDetails(
                      request.categoryId(),
                      name,
                      request.description(),
                      request.price(),
                      request.stockQuantity()
                  );
                  return product;
                }))
            )
        )
        .flatMap(productRepository::save)
        .doOnNext(product -> log.info(
            "Product updated productId={} userId={} categoryId={} status={}",
            product.getId(),
            product.getSellerId(),
            product.getCategoryId(),
            product.getStatus()
        ))
        .map(this::toResponse);
  }

  public Mono<ProductResponse> activateProduct(UUID productId) {
    return findRequired(productId)
        .map(product -> {
          product.activate();
          return product;
        })
        .flatMap(productRepository::save)
        .doOnNext(product -> log.info("Product activated productId={} status={}", product.getId(), product.getStatus()))
        .map(this::toResponse);
  }

  public Mono<ProductResponse> deactivateProduct(UUID productId) {
    return findRequired(productId)
        .map(product -> {
          product.deactivate();
          return product;
        })
        .flatMap(productRepository::save)
        .doOnNext(product -> log.info("Product deactivated productId={} status={}", product.getId(), product.getStatus()))
        .map(this::toResponse);
  }

  public Mono<PageResponse<ProductResponse>> getActiveProducts(PageRequest pageRequest) {
    Mono<List<ProductResponse>> content = productRepository.findAllActive(pageRequest)
        .map(this::toResponse)
        .collectList();
    Mono<Long> totalElements = productRepository.countAllActive();

    return Mono.zip(content, totalElements)
        .map(result -> PageResponse.of(result.getT1(), pageRequest, result.getT2()));
  }

  public Mono<ProductResponse> getProductById(UUID productId) {
    return findRequired(productId)
        .filter(product -> product.getStatus() == ProductStatus.ACTIVE)
        .switchIfEmpty(Mono.error(() -> new ProductNotFoundException(productId)))
        .map(this::toResponse);
  }

  public Mono<PageResponse<ProductResponse>> getCurrentSellerProducts(PageRequest pageRequest) {
    return currentUserProvider.currentUserId()
        .flatMap(sellerId -> {
          Mono<List<ProductResponse>> content = productRepository.findBySellerId(sellerId, pageRequest)
              .map(this::toResponse)
              .collectList();
          Mono<Long> totalElements = productRepository.countBySellerId(sellerId);

          return Mono.zip(content, totalElements)
              .map(result -> PageResponse.of(result.getT1(), pageRequest, result.getT2()));
        });
  }

  private Mono<Void> requireCategory(UUID categoryId) {
    return categoryRepository.findById(categoryId)
        .switchIfEmpty(Mono.error(() -> new CategoryNotFoundException(categoryId)))
        .then();
  }

  private Mono<Product> findRequired(UUID productId) {
    return productRepository.findById(productId)
        .switchIfEmpty(Mono.error(() -> new ProductNotFoundException(productId)));
  }

  private Mono<Void> requireOwner(Product product, UUID sellerId) {
    if (!product.getSellerId().equals(sellerId)) {
      log.warn("Product access denied productId={} userId={}", product.getId(), sellerId);
      return Mono.error(new ProductAccessDeniedException(product.getId()));
    }
    return Mono.empty();
  }

  private ProductResponse toResponse(Product product) {
    return new ProductResponse(
        product.getId(),
        product.getSellerId(),
        product.getCategoryId(),
        product.getName(),
        product.getDescription(),
        product.getPrice(),
        product.getStockQuantity(),
        product.getStatus(),
        product.getCreatedAt(),
        product.getUpdatedAt()
    );
  }
}
