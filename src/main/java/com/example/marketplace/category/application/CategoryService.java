package com.example.marketplace.category.application;

import com.example.marketplace.category.api.CategoryResponse;
import com.example.marketplace.category.api.CreateCategoryRequest;
import com.example.marketplace.category.domain.Category;
import com.example.marketplace.category.domain.CategoryRepository;
import com.example.marketplace.common.exception.CategoryNotFoundException;
import com.example.marketplace.common.exception.DuplicateCategoryNameException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CategoryService {

  private final CategoryRepository categoryRepository;

  public Mono<CategoryResponse> createCategory(CreateCategoryRequest request) {
    String name = request.name().trim();

    return categoryRepository.existsByNameIgnoreCase(name)
        .flatMap(exists -> {
          if (exists) {
            log.warn("Duplicate category creation attempt");
            return Mono.error(new DuplicateCategoryNameException(name));
          }
          return categoryRepository.save(Category.createNew(name, request.description()));
        })
        .onErrorMap(DuplicateKeyException.class, exception -> new DuplicateCategoryNameException(name))
        .doOnNext(category -> log.info("Category created categoryId={}", category.getId()))
        .map(this::toResponse);
  }

  public Flux<CategoryResponse> getAllCategories() {
    return categoryRepository.findAll()
        .map(this::toResponse);
  }

  public Mono<CategoryResponse> getCategoryById(UUID id) {
    return categoryRepository.findById(id)
        .switchIfEmpty(Mono.error(() -> new CategoryNotFoundException(id)))
        .map(this::toResponse);
  }

  private CategoryResponse toResponse(Category category) {
    return new CategoryResponse(
        category.getId(),
        category.getName(),
        category.getDescription(),
        category.getCreatedAt(),
        category.getUpdatedAt()
    );
  }
}
