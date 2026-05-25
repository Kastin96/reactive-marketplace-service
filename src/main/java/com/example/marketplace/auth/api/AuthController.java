package com.example.marketplace.auth.api;

import com.example.marketplace.auth.application.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Public registration and login endpoints.")
public class AuthController {

  private final AuthService authService;

  @PostMapping("/register/customer")
  @Operation(summary = "Register customer", description = "Public endpoint. Creates an active CUSTOMER account and returns a JWT access token.")
  public Mono<AuthResponse> registerCustomer(@Valid @RequestBody RegisterRequest request) {
    return authService.registerCustomer(request);
  }

  @PostMapping("/register/seller")
  @Operation(summary = "Register seller", description = "Public endpoint. Creates an active SELLER account and returns a JWT access token.")
  public Mono<AuthResponse> registerSeller(@Valid @RequestBody RegisterRequest request) {
    return authService.registerSeller(request);
  }

  @PostMapping("/login")
  @Operation(summary = "Login", description = "Public endpoint. Authenticates an active user and returns a JWT access token.")
  public Mono<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
    return authService.login(request);
  }
}
