package com.example.marketplace.user.api;

import com.example.marketplace.config.OpenApiConfig;
import com.example.marketplace.security.CurrentUserProvider;
import com.example.marketplace.user.application.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "Authenticated user profile endpoints.")
public class UserController {

  private final CurrentUserProvider currentUserProvider;
  private final UserService userService;

  @GetMapping("/me")
  @Operation(
      summary = "Get current user profile",
      description = "Requires authenticated CUSTOMER, SELLER, or ADMIN user.",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH_SCHEME)
  )
  public Mono<UserProfileResponse> me() {
    return currentUserProvider.currentUserId()
        .flatMap(userService::getById)
        .map(user -> new UserProfileResponse(
            user.getId(),
            user.getEmail(),
            user.getRole(),
            user.getStatus()
        ));
  }
}
