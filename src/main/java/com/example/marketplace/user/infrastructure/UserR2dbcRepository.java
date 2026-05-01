package com.example.marketplace.user.infrastructure;

import java.util.UUID;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

interface UserR2dbcRepository extends ReactiveCrudRepository<UserEntity, UUID> {

  Mono<UserEntity> findByEmail(String email);

  Mono<Boolean> existsByEmail(String email);
}
