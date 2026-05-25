package com.example.marketplace.category.infrastructure;

import com.example.marketplace.PostgresTestContainerConfig;
import com.example.marketplace.category.domain.Category;
import com.example.marketplace.category.domain.CategoryRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import reactor.test.StepVerifier;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SpringBootTest
class CategoryRepositoryIntegrationTests {

  @DynamicPropertySource
  static void postgresProperties(DynamicPropertyRegistry registry) {
    PostgresTestContainerConfig.configure(registry);
  }

  @Autowired
  private CategoryRepository categoryRepository;

  @Test
  void saveCategory() {
    Category category = newCategory("save");

    StepVerifier.create(categoryRepository.save(category))
        .assertNext(saved -> {
          assertThat(saved.getId()).isEqualTo(category.getId());
          assertThat(saved.getName()).isEqualTo(category.getName());
          assertThat(saved.getDescription()).isEqualTo(category.getDescription());
          assertThat(saved.getCreatedAt()).isEqualTo(category.getCreatedAt());
          assertThat(saved.getUpdatedAt()).isEqualTo(category.getUpdatedAt());
        })
        .verifyComplete();
  }

  @Test
  void findCategoryById() {
    Category category = newCategory("find-id");

    StepVerifier.create(categoryRepository.save(category).flatMap(saved -> categoryRepository.findById(saved.getId())))
        .assertNext(found -> {
          assertThat(found.getId()).isEqualTo(category.getId());
          assertThat(found.getName()).isEqualTo(category.getName());
        })
        .verifyComplete();
  }

  @Test
  void findAllCategories() {
    Category first = newCategory("find-all-one");
    Category second = newCategory("find-all-two");

    StepVerifier.create(categoryRepository.save(first)
            .then(categoryRepository.save(second))
            .thenMany(categoryRepository.findAll().filter(category ->
                category.getId().equals(first.getId()) || category.getId().equals(second.getId())
            )))
        .expectNextCount(2)
        .verifyComplete();
  }

  @Test
  void existsByNameCaseInsensitively() {
    Category category = newCategory("case-insensitive");

    StepVerifier.create(categoryRepository.save(category)
            .then(categoryRepository.existsByNameIgnoreCase(category.getName().toUpperCase())))
        .expectNext(true)
        .verifyComplete();
  }

  private Category newCategory(String prefix) {
    return Category.createNew(
        prefix + "-" + UUID.randomUUID(),
        "Test category"
    );
  }
}
