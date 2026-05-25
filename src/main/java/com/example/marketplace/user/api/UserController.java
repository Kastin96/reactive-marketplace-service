package com.example.marketplace.user.api;

import com.example.marketplace.security.CurrentUserProvider;
import com.example.marketplace.user.application.UserService;
import com.example.marketplace.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

  private final CurrentUserProvider currentUserProvider;
  private final UserService userService;

  @GetMapping("/me")
  public Mono<UserProfileResponse> me() {
    return currentUserProvider.currentUserId()
        .flatMap(userService::getById)
        .map(this::toResponse);
  }

  private UserProfileResponse toResponse(User user) {
    return new UserProfileResponse(
        user.getId(),
        user.getEmail(),
        user.getRole(),
        user.getStatus()
    );
  }
}
