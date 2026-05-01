package com.example.marketplace.user.application;

import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.example.marketplace.common.exception.UserNotFoundException;
import com.example.marketplace.user.domain.User;
import com.example.marketplace.user.domain.UserRepository;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class UserService {

  private final UserRepository userRepository;

  public Mono<User> getById(UUID id) {
    return userRepository.findById(id)
        .switchIfEmpty(Mono.error(() -> new UserNotFoundException("User not found with id: " + id)));
  }

  public Mono<User> getByEmail(String email) {
    return userRepository.findByEmail(email)
        .switchIfEmpty(Mono.error(() -> new UserNotFoundException("User not found with email: " + email)));
  }

  public Mono<Boolean> existsByEmail(String email) {
    return userRepository.existsByEmail(email);
  }
}
