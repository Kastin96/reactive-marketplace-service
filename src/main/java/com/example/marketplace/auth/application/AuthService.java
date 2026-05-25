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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Locale;

@Service
@RequiredArgsConstructor
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
        .switchIfEmpty(Mono.error(InvalidCredentialsException::new))
        .flatMap(user -> authenticate(user, request.password()));
  }

  private Mono<AuthResponse> register(RegisterRequest request, UserRole role) {
    String email = normalizeEmail(request.email());

    return userRepository.existsByEmail(email)
        .flatMap(exists -> exists
            ? Mono.error(new EmailAlreadyExistsException(email))
            : createUser(request, role, email)
        );
  }

  private Mono<AuthResponse> createUser(RegisterRequest request, UserRole role, String email) {
    String passwordHash = passwordEncoder.encode(request.password());
    User user = User.createNewActiveUser(email, passwordHash, role);

    return userRepository.save(user)
        .map(this::toAuthResponse);
  }

  private Mono<AuthResponse> authenticate(User user, String rawPassword) {
    if (!passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
      return Mono.error(new InvalidCredentialsException());
    }

    if (user.getStatus() == UserStatus.BLOCKED) {
      return Mono.error(new UserBlockedException(user.getId()));
    }

    return Mono.just(toAuthResponse(user));
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
