package com.example.marketplace.user.domain;

import java.util.UUID;

import reactor.core.publisher.Mono;

public interface UserRepository {

  Mono<User> save(User user);

  Mono<User> findById(UUID id);

  Mono<User> findByEmail(String email);

  Mono<Boolean> existsByEmail(String email);
}
