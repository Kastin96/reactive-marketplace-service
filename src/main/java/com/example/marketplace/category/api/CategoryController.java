package com.example.marketplace.category.api;

import com.example.marketplace.category.application.CategoryService;
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
public class CategoryController {

  private final CategoryService categoryService;

  @PostMapping("/api/v1/admin/categories")
  @ResponseStatus(HttpStatus.CREATED)
  public Mono<CategoryResponse> createCategory(@Valid @RequestBody CreateCategoryRequest request) {
    return categoryService.createCategory(request);
  }

  @GetMapping("/api/v1/categories")
  public Flux<CategoryResponse> getAllCategories() {
    return categoryService.getAllCategories();
  }

  @GetMapping("/api/v1/categories/{categoryId}")
  public Mono<CategoryResponse> getCategoryById(@PathVariable UUID categoryId) {
    return categoryService.getCategoryById(categoryId);
  }
}
