package com.example.marketplace.category.api;

import com.example.marketplace.category.application.CategoryService;
import com.example.marketplace.config.OpenApiConfig;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Tag(name = "Categories", description = "Category catalog endpoints.")
public class CategoryController {

  private final CategoryService categoryService;

  @PostMapping("/api/v1/admin/categories")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
      summary = "Create category",
      description = "Requires ADMIN role.",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH_SCHEME)
  )
  public Mono<CategoryResponse> createCategory(@Valid @RequestBody CreateCategoryRequest request) {
    return categoryService.createCategory(request);
  }

  @GetMapping("/api/v1/categories")
  @Operation(
      summary = "List categories",
      description = "Requires authenticated CUSTOMER, SELLER, or ADMIN user.",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH_SCHEME)
  )
  public Flux<CategoryResponse> getAllCategories() {
    return categoryService.getAllCategories();
  }

  @GetMapping("/api/v1/categories/{categoryId}")
  @Operation(
      summary = "Get category",
      description = "Requires authenticated CUSTOMER, SELLER, or ADMIN user.",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH_SCHEME)
  )
  public Mono<CategoryResponse> getCategoryById(@PathVariable UUID categoryId) {
    return categoryService.getCategoryById(categoryId);
  }
}
