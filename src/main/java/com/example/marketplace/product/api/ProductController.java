package com.example.marketplace.product.api;

import com.example.marketplace.common.pagination.PageRequest;
import com.example.marketplace.common.pagination.PageResponse;
import com.example.marketplace.config.OpenApiConfig;
import com.example.marketplace.product.application.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Tag(name = "Products", description = "Product catalog and seller/admin product management endpoints.")
public class ProductController {

  private final ProductService productService;

  @PostMapping("/api/v1/seller/products")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
      summary = "Create product",
      description = "Requires SELLER role. New products are created as DRAFT.",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH_SCHEME)
  )
  public Mono<ProductResponse> createProduct(@Valid @RequestBody CreateProductRequest request) {
    return productService.createProduct(request);
  }

  @PutMapping("/api/v1/seller/products/{productId}")
  @Operation(
      summary = "Update own product",
      description = "Requires SELLER role. Sellers can update only products they own.",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH_SCHEME)
  )
  public Mono<ProductResponse> updateOwnProduct(
      @PathVariable UUID productId,
      @Valid @RequestBody UpdateProductRequest request
  ) {
    return productService.updateOwnProduct(productId, request);
  }

  @GetMapping("/api/v1/seller/products")
  @Operation(
      summary = "List own products",
      description = "Requires SELLER role. Returns a paged response ordered by newest product first.",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH_SCHEME)
  )
  public Mono<PageResponse<ProductResponse>> getCurrentSellerProducts(
      @Parameter(description = "Zero-based page index.", example = "0")
      @RequestParam(defaultValue = PageRequest.DEFAULT_PAGE_VALUE) int page,
      @Parameter(description = "Page size from 1 to 100.", example = "20")
      @RequestParam(defaultValue = PageRequest.DEFAULT_SIZE_VALUE) int size
  ) {
    return productService.getCurrentSellerProducts(pageRequest(page, size));
  }

  @GetMapping("/api/v1/products")
  @Operation(
      summary = "List active products",
      description = "Requires authenticated CUSTOMER, SELLER, or ADMIN user. Returns a paged response ordered by newest product first.",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH_SCHEME)
  )
  public Mono<PageResponse<ProductResponse>> getActiveProducts(
      @Parameter(description = "Zero-based page index.", example = "0")
      @RequestParam(defaultValue = PageRequest.DEFAULT_PAGE_VALUE) int page,
      @Parameter(description = "Page size from 1 to 100.", example = "20")
      @RequestParam(defaultValue = PageRequest.DEFAULT_SIZE_VALUE) int size
  ) {
    return productService.getActiveProducts(pageRequest(page, size));
  }

  @GetMapping("/api/v1/products/{productId}")
  @Operation(
      summary = "Get active product",
      description = "Requires authenticated CUSTOMER, SELLER, or ADMIN user. Inactive products are hidden.",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH_SCHEME)
  )
  public Mono<ProductResponse> getProductById(@PathVariable UUID productId) {
    return productService.getProductById(productId);
  }

  @PatchMapping("/api/v1/admin/products/{productId}/activate")
  @Operation(
      summary = "Activate product",
      description = "Requires ADMIN role.",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH_SCHEME)
  )
  public Mono<ProductResponse> activateProduct(@PathVariable UUID productId) {
    return productService.activateProduct(productId);
  }

  @PatchMapping("/api/v1/admin/products/{productId}/deactivate")
  @Operation(
      summary = "Deactivate product",
      description = "Requires ADMIN role.",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH_SCHEME)
  )
  public Mono<ProductResponse> deactivateProduct(@PathVariable UUID productId) {
    return productService.deactivateProduct(productId);
  }

  private PageRequest pageRequest(int page, int size) {
    return PageRequest.of(page, size);
  }
}
