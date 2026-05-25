package com.example.marketplace.user.application;

import com.example.marketplace.common.exception.UserNotFoundException;
import com.example.marketplace.user.domain.User;
import com.example.marketplace.user.domain.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

  private final UserRepository userRepository;

  public Mono<User> getById(UUID id) {
    return userRepository.findById(id)
        .switchIfEmpty(Mono.error(() -> new UserNotFoundException("User not found with id: " + id)));
  }
}
