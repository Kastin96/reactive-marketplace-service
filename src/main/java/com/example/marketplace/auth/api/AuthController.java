package com.example.marketplace.auth.api;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.example.marketplace.auth.application.AuthService;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

  private final AuthService authService;

  @PostMapping("/register/customer")
  public Mono<AuthResponse> registerCustomer(@Valid @RequestBody RegisterRequest request) {
    return authService.registerCustomer(request);
  }

  @PostMapping("/register/seller")
  public Mono<AuthResponse> registerSeller(@Valid @RequestBody RegisterRequest request) {
    return authService.registerSeller(request);
  }

  @PostMapping("/login")
  public Mono<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
    return authService.login(request);
  }
}
