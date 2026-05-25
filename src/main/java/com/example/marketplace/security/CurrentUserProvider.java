package com.example.marketplace.security;

import java.util.UUID;

import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Component;
import com.example.marketplace.user.domain.UserRole;
import reactor.core.publisher.Mono;

@Component
public class CurrentUserProvider {

  public Mono<UUID> currentUserId() {
    return currentUser().map(AuthenticatedUser::id);
  }

  public Mono<String> currentUserEmail() {
    return currentUser().map(AuthenticatedUser::email);
  }

  public Mono<UserRole> currentUserRole() {
    return currentUser().map(AuthenticatedUser::role);
  }

  private Mono<AuthenticatedUser> currentUser() {
    return ReactiveSecurityContextHolder.getContext()
        .flatMap(securityContext -> Mono.justOrEmpty(securityContext.getAuthentication()))
        .filter(Authentication::isAuthenticated)
        .map(Authentication::getPrincipal)
        .ofType(AuthenticatedUser.class)
        .switchIfEmpty(Mono.error(new AuthenticationCredentialsNotFoundException("User is not authenticated")));
  }
}
