package com.example.marketplace.auth.api;

import com.example.marketplace.PostgresTestContainerConfig;
import com.example.marketplace.user.domain.User;
import com.example.marketplace.user.domain.UserRepository;
import com.example.marketplace.user.domain.UserRole;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.test.StepVerifier;

import java.util.UUID;

@ActiveProfiles("test")
@AutoConfigureWebTestClient
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AuthControllerIntegrationTests {

  @DynamicPropertySource
  static void postgresProperties(DynamicPropertyRegistry registry) {
    PostgresTestContainerConfig.configure(registry);
  }

  @Autowired
  private WebTestClient webTestClient;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private PasswordEncoder passwordEncoder;

  @Test
  void customerRegistrationReturnsToken() {
    RegisterRequest request = new RegisterRequest(uniqueEmail("customer"), "password123");

    webTestClient.post()
        .uri("/api/v1/auth/register/customer")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(request)
        .exchange()
        .expectStatus().isOk()
        .expectBody()
        .jsonPath("$.accessToken").isNotEmpty()
        .jsonPath("$.tokenType").isEqualTo("Bearer")
        .jsonPath("$.expiresIn").isEqualTo(900)
        .jsonPath("$.user.email").isEqualTo(request.email())
        .jsonPath("$.user.role").isEqualTo("CUSTOMER")
        .jsonPath("$.user.status").isEqualTo("ACTIVE");
  }

  @Test
  void sellerRegistrationReturnsToken() {
    RegisterRequest request = new RegisterRequest(uniqueEmail("seller"), "password123");

    webTestClient.post()
        .uri("/api/v1/auth/register/seller")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(request)
        .exchange()
        .expectStatus().isOk()
        .expectBody()
        .jsonPath("$.accessToken").isNotEmpty()
        .jsonPath("$.tokenType").isEqualTo("Bearer")
        .jsonPath("$.user.email").isEqualTo(request.email())
        .jsonPath("$.user.role").isEqualTo("SELLER")
        .jsonPath("$.user.status").isEqualTo("ACTIVE");
  }

  @Test
  void duplicateRegistrationReturnsConflict() {
    String email = uniqueEmail("duplicate");
    User user = User.createNewActiveUser(email, passwordEncoder.encode("password123"), UserRole.CUSTOMER);

    StepVerifier.create(userRepository.save(user))
        .expectNextMatches(saved -> saved.getId().equals(user.getId()))
        .verifyComplete();

    webTestClient.post()
        .uri("/api/v1/auth/register/customer")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(new RegisterRequest(email, "password123"))
        .exchange()
        .expectStatus().isEqualTo(409)
        .expectBody()
        .jsonPath("$.status").isEqualTo(409)
        .jsonPath("$.error").isEqualTo("Conflict")
        .jsonPath("$.message").isEqualTo("Email already exists: " + email)
        .jsonPath("$.path").isEqualTo("/api/v1/auth/register/customer");
  }

  @Test
  void loginReturnsToken() {
    String email = uniqueEmail("login");
    User user = User.createNewActiveUser(email, passwordEncoder.encode("password123"), UserRole.CUSTOMER);

    StepVerifier.create(userRepository.save(user))
        .expectNextMatches(saved -> saved.getId().equals(user.getId()))
        .verifyComplete();

    webTestClient.post()
        .uri("/api/v1/auth/login")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(new LoginRequest(email, "password123"))
        .exchange()
        .expectStatus().isOk()
        .expectBody()
        .jsonPath("$.accessToken").isNotEmpty()
        .jsonPath("$.tokenType").isEqualTo("Bearer")
        .jsonPath("$.user.email").isEqualTo(email)
        .jsonPath("$.user.role").isEqualTo("CUSTOMER");
  }

  @Test
  void invalidEmailReturnsValidationError() {
    RegisterRequest request = new RegisterRequest("not-an-email", "password123");

    webTestClient.post()
        .uri("/api/v1/auth/register/customer")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(request)
        .exchange()
        .expectStatus().isBadRequest();
  }

  @Test
  void shortPasswordReturnsValidationError() {
    RegisterRequest request = new RegisterRequest(uniqueEmail("short"), "short");

    webTestClient.post()
        .uri("/api/v1/auth/register/customer")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(request)
        .exchange()
        .expectStatus().isBadRequest();
  }

  private String uniqueEmail(String prefix) {
    return prefix + "-" + UUID.randomUUID() + "@example.com";
  }
}
