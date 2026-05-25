package com.example.marketplace.category.application;

import com.example.marketplace.category.api.CreateCategoryRequest;
import com.example.marketplace.category.domain.Category;
import com.example.marketplace.category.domain.CategoryRepository;
import com.example.marketplace.common.exception.CategoryNotFoundException;
import com.example.marketplace.common.exception.DuplicateCategoryNameException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTests {

  @Mock
  private CategoryRepository categoryRepository;

  private CategoryService categoryService;

  @BeforeEach
  void setUp() {
    categoryService = new CategoryService(categoryRepository);
  }

  @Test
  void createCategorySuccessfully() {
    CreateCategoryRequest request = new CreateCategoryRequest(" Electronics ", "Devices and accessories");
    when(categoryRepository.existsByNameIgnoreCase("Electronics")).thenReturn(Mono.just(false));
    when(categoryRepository.save(any(Category.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

    StepVerifier.create(categoryService.createCategory(request))
        .assertNext(response -> {
          assertThat(response.id()).isNotNull();
          assertThat(response.name()).isEqualTo("Electronics");
          assertThat(response.description()).isEqualTo("Devices and accessories");
          assertThat(response.createdAt()).isNotNull();
          assertThat(response.updatedAt()).isNotNull();
        })
        .verifyComplete();

    ArgumentCaptor<Category> categoryCaptor = ArgumentCaptor.forClass(Category.class);
    verify(categoryRepository).save(categoryCaptor.capture());
    assertThat(categoryCaptor.getValue().getName()).isEqualTo("Electronics");
  }

  @Test
  void createCategoryRejectsDuplicateName() {
    CreateCategoryRequest request = new CreateCategoryRequest("electronics", null);
    when(categoryRepository.existsByNameIgnoreCase("electronics")).thenReturn(Mono.just(true));

    StepVerifier.create(categoryService.createCategory(request))
        .expectError(DuplicateCategoryNameException.class)
        .verify();
  }

  @Test
  void createCategoryMapsDuplicateNameConstraintToConflictException() {
    CreateCategoryRequest request = new CreateCategoryRequest("Electronics", null);
    when(categoryRepository.existsByNameIgnoreCase("Electronics")).thenReturn(Mono.just(false));
    when(categoryRepository.save(any(Category.class))).thenReturn(Mono.error(new DuplicateKeyException("uk_categories_name")));

    StepVerifier.create(categoryService.createCategory(request))
        .expectErrorSatisfies(error -> {
          assertThat(error).isInstanceOf(DuplicateCategoryNameException.class);
          assertThat(error).hasMessage("Category name already exists: Electronics");
        })
        .verify();
  }

  @Test
  void getCategoryByIdSuccessfully() {
    Category category = Category.createNew("Books", "Printed books");
    when(categoryRepository.findById(category.getId())).thenReturn(Mono.just(category));

    StepVerifier.create(categoryService.getCategoryById(category.getId()))
        .assertNext(response -> {
          assertThat(response.id()).isEqualTo(category.getId());
          assertThat(response.name()).isEqualTo(category.getName());
          assertThat(response.description()).isEqualTo(category.getDescription());
        })
        .verifyComplete();
  }

  @Test
  void getCategoryByIdThrowsNotFoundWhenCategoryDoesNotExist() {
    UUID categoryId = UUID.randomUUID();
    when(categoryRepository.findById(categoryId)).thenReturn(Mono.empty());

    StepVerifier.create(categoryService.getCategoryById(categoryId))
        .expectError(CategoryNotFoundException.class)
        .verify();
  }
}
