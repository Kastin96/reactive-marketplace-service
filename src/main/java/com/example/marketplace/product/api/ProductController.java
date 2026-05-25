package com.example.marketplace.product.api;

import com.example.marketplace.product.application.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class ProductController {

  private final ProductService productService;

  @PostMapping("/api/v1/seller/products")
  @ResponseStatus(HttpStatus.CREATED)
  public Mono<ProductResponse> createProduct(@Valid @RequestBody CreateProductRequest request) {
    return productService.createProduct(request);
  }

  @PutMapping("/api/v1/seller/products/{productId}")
  public Mono<ProductResponse> updateOwnProduct(
      @PathVariable UUID productId,
      @Valid @RequestBody UpdateProductRequest request
  ) {
    return productService.updateOwnProduct(productId, request);
  }

  @GetMapping("/api/v1/seller/products")
  public Flux<ProductResponse> getCurrentSellerProducts() {
    return productService.getCurrentSellerProducts();
  }

  @GetMapping("/api/v1/products")
  public Flux<ProductResponse> getActiveProducts() {
    return productService.getActiveProducts();
  }

  @GetMapping("/api/v1/products/{productId}")
  public Mono<ProductResponse> getProductById(@PathVariable UUID productId) {
    return productService.getProductById(productId);
  }

  @PatchMapping("/api/v1/admin/products/{productId}/activate")
  public Mono<ProductResponse> activateProduct(@PathVariable UUID productId) {
    return productService.activateProduct(productId);
  }

  @PatchMapping("/api/v1/admin/products/{productId}/deactivate")
  public Mono<ProductResponse> deactivateProduct(@PathVariable UUID productId) {
    return productService.deactivateProduct(productId);
  }
}
