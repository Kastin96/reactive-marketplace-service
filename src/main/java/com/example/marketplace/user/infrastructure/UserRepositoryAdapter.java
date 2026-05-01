package com.example.marketplace.user.infrastructure;

import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.stereotype.Repository;
import com.example.marketplace.user.domain.User;
import com.example.marketplace.user.domain.UserRepository;
import com.example.marketplace.user.domain.UserRole;
import com.example.marketplace.user.domain.UserStatus;
import reactor.core.publisher.Mono;

@Repository
@RequiredArgsConstructor
class UserRepositoryAdapter implements UserRepository {

  private final UserR2dbcRepository repository;
  private final R2dbcEntityTemplate entityTemplate;

  @Override
  public Mono<User> save(User user) {
    UserEntity entity = toEntity(user);

    return repository.existsById(user.getId())
        .flatMap(exists -> exists
            ? repository.save(entity)
            : entityTemplate.insert(UserEntity.class).using(entity)
        )
        .map(this::toDomain);
  }

  @Override
  public Mono<User> findById(UUID id) {
    return repository.findById(id)
        .map(this::toDomain);
  }

  @Override
  public Mono<User> findByEmail(String email) {
    return repository.findByEmail(email)
        .map(this::toDomain);
  }

  @Override
  public Mono<Boolean> existsByEmail(String email) {
    return repository.existsByEmail(email);
  }

  private UserEntity toEntity(User user) {
    return new UserEntity(
        user.getId(),
        user.getEmail(),
        user.getPasswordHash(),
        user.getRole().name(),
        user.getStatus().name(),
        user.getCreatedAt(),
        user.getUpdatedAt()
    );
  }

  private User toDomain(UserEntity entity) {
    return User.restore(
        entity.id(),
        entity.email(),
        entity.passwordHash(),
        UserRole.valueOf(entity.role()),
        UserStatus.valueOf(entity.status()),
        entity.createdAt(),
        entity.updatedAt()
    );
  }
}
