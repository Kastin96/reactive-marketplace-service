package com.example.marketplace.security;

import com.example.marketplace.user.application.UserService;
import com.example.marketplace.user.domain.UserStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.List;

@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationWebFilter implements WebFilter {

  private static final String BEARER_PREFIX = "Bearer ";

  private final JwtTokenProvider jwtTokenProvider;
  private final UserService userService;

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
    String authorizationHeader = exchange.getRequest()
        .getHeaders()
        .getFirst(HttpHeaders.AUTHORIZATION);

    if (authorizationHeader == null || authorizationHeader.isBlank()) {
      return chain.filter(exchange);
    }

    if (!authorizationHeader.startsWith(BEARER_PREFIX)) {
      log.warn("Invalid JWT authentication attempt reason=invalid_authorization_scheme");
      return unauthorized(exchange);
    }

    String token = authorizationHeader.substring(BEARER_PREFIX.length());

    JwtAuthenticationClaims claims;
    try {
      claims = jwtTokenProvider.extractAuthenticationClaims(token);
    } catch (RuntimeException exception) {
      log.warn("Invalid JWT authentication attempt reason=invalid_token");
      return unauthorized(exchange);
    }

    return userService.getById(claims.userId())
        .filter(user -> user.getStatus() == UserStatus.ACTIVE)
        .switchIfEmpty(Mono.error(InvalidJwtAuthenticationException::new))
        .onErrorMap(exception -> new InvalidJwtAuthenticationException())
        .flatMap(user -> {
          AuthenticatedUser principal = new AuthenticatedUser(
              user.getId(),
              user.getEmail(),
              user.getRole()
          );
          UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
              principal,
              token,
              List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
          );

          return chain.filter(exchange)
              .contextWrite(ReactiveSecurityContextHolder.withAuthentication(authentication));
        })
        .onErrorResume(InvalidJwtAuthenticationException.class, exception -> {
          log.warn("Invalid JWT authentication attempt userId={}", claims.userId());
          return unauthorized(exchange);
        });
  }

  private Mono<Void> unauthorized(ServerWebExchange exchange) {
    exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
    return exchange.getResponse().setComplete();
  }

  private static final class InvalidJwtAuthenticationException extends RuntimeException {
  }
}
