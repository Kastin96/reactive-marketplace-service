package com.example.marketplace.auth.application;

import com.example.marketplace.auth.api.AuthResponse;
import com.example.marketplace.auth.api.AuthUserResponse;
import com.example.marketplace.auth.api.LoginRequest;
import com.example.marketplace.auth.api.RegisterRequest;
import com.example.marketplace.common.exception.EmailAlreadyExistsException;
import com.example.marketplace.common.exception.InvalidCredentialsException;
import com.example.marketplace.common.exception.UserBlockedException;
import com.example.marketplace.security.JwtTokenProvider;
import com.example.marketplace.user.domain.User;
import com.example.marketplace.user.domain.UserRepository;
import com.example.marketplace.user.domain.UserRole;
import com.example.marketplace.user.domain.UserStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Locale;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

  private static final String TOKEN_TYPE = "Bearer";

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final JwtTokenProvider jwtTokenProvider;

  public Mono<AuthResponse> registerCustomer(RegisterRequest request) {
    return register(request, UserRole.CUSTOMER);
  }

  public Mono<AuthResponse> registerSeller(RegisterRequest request) {
    return register(request, UserRole.SELLER);
  }

  public Mono<AuthResponse> login(LoginRequest request) {
    String email = normalizeEmail(request.email());

    return userRepository.findByEmail(email)
        .switchIfEmpty(Mono.defer(() -> {
          log.warn("Invalid login attempt");
          return Mono.error(new InvalidCredentialsException());
        }))
        .flatMap(user -> {
          if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            log.warn("Invalid login attempt userId={} role={}", user.getId(), user.getRole());
            return Mono.error(new InvalidCredentialsException());
          }

          if (user.getStatus() == UserStatus.BLOCKED) {
            log.warn("Blocked user authentication attempt userId={} role={}", user.getId(), user.getRole());
            return Mono.error(new UserBlockedException(user.getId()));
          }

          return Mono.just(toAuthResponse(user));
        })
        .doOnNext(response -> log.info(
            "User logged in userId={} role={}",
            response.user().id(),
            response.user().role()
        ));
  }

  private Mono<AuthResponse> register(RegisterRequest request, UserRole role) {
    String email = normalizeEmail(request.email());

    return userRepository.existsByEmail(email)
        .flatMap(exists -> {
          if (exists) {
            log.warn("Duplicate registration attempt role={}", role);
            return Mono.error(new EmailAlreadyExistsException(email));
          }
          String passwordHash = passwordEncoder.encode(request.password());
          User user = User.createNewActiveUser(email, passwordHash, role);

          return userRepository.save(user)
              .onErrorMap(DuplicateKeyException.class, exception -> new EmailAlreadyExistsException(email))
              .doOnNext(savedUser -> log.info("User registered userId={} role={}", savedUser.getId(), savedUser.getRole()))
              .map(this::toAuthResponse);
        });
  }

  private AuthResponse toAuthResponse(User user) {
    return new AuthResponse(
        jwtTokenProvider.generateAccessToken(user),
        TOKEN_TYPE,
        jwtTokenProvider.expiresInSeconds(),
        new AuthUserResponse(
            user.getId(),
            user.getEmail(),
            user.getRole(),
            user.getStatus()
        )
    );
  }

  private String normalizeEmail(String email) {
    return email.trim().toLowerCase(Locale.ROOT);
  }
}
