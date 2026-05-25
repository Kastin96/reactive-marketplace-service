package com.example.marketplace.user.infrastructure;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

import java.util.UUID;

interface UserR2dbcRepository extends ReactiveCrudRepository<UserEntity, UUID> {

  Mono<UserEntity> findByEmail(String email);

  Mono<Boolean> existsByEmail(String email);
}
