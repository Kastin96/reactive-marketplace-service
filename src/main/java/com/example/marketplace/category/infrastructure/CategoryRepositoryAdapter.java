package com.example.marketplace.category.infrastructure;

import com.example.marketplace.category.domain.Category;
import com.example.marketplace.category.domain.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Repository
@RequiredArgsConstructor
class CategoryRepositoryAdapter implements CategoryRepository {

  private final CategoryR2dbcRepository repository;
  private final R2dbcEntityTemplate entityTemplate;

  @Override
  public Mono<Category> save(Category category) {
    CategoryEntity entity = toEntity(category);

    return repository.existsById(category.getId())
        .flatMap(exists -> exists
            ? repository.save(entity)
            : entityTemplate.insert(CategoryEntity.class).using(entity)
        )
        .map(this::toDomain);
  }

  @Override
  public Mono<Category> findById(UUID id) {
    return repository.findById(id)
        .map(this::toDomain);
  }

  @Override
  public Flux<Category> findAll() {
    return repository.findAll()
        .map(this::toDomain);
  }

  @Override
  public Mono<Boolean> existsByNameIgnoreCase(String name) {
    return repository.existsByNameIgnoreCase(name);
  }

  private CategoryEntity toEntity(Category category) {
    return new CategoryEntity(
        category.getId(),
        category.getName(),
        category.getDescription(),
        category.getCreatedAt(),
        category.getUpdatedAt()
    );
  }

  private Category toDomain(CategoryEntity entity) {
    return Category.restore(
        entity.id(),
        entity.name(),
        entity.description(),
        entity.createdAt(),
        entity.updatedAt()
    );
  }
}
