package com.example.marketplace.user.domain;

import reactor.core.publisher.Mono;

import java.util.UUID;

public interface UserRepository {

  Mono<User> save(User user);

  Mono<User> findById(UUID id);

  Mono<User> findByEmail(String email);

  Mono<Boolean> existsByEmail(String email);
}
