package com.example.marketplace.security;

import com.example.marketplace.PostgresTestContainerConfig;
import com.example.marketplace.auth.api.LoginRequest;
import com.example.marketplace.user.domain.User;
import com.example.marketplace.user.domain.UserRepository;
import com.example.marketplace.user.domain.UserRole;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.test.StepVerifier;

import java.util.UUID;

@ActiveProfiles("test")
@AutoConfigureWebTestClient
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(SecurityAuthorizationIntegrationTests.TestRoleEndpoints.class)
class SecurityAuthorizationIntegrationTests {

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

  @Autowired
  private JwtTokenProvider jwtTokenProvider;

  @Test
  void actuatorHealthReturnsOkWithoutToken() {
    webTestClient.get()
        .uri("/actuator/health")
        .exchange()
        .expectStatus().isOk();
  }

  @Test
  void loginEndpointIsAccessibleWithoutToken() {
    webTestClient.post()
        .uri("/api/v1/auth/login")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(new LoginRequest("invalid-email", "password123"))
        .exchange()
        .expectStatus().isBadRequest();
  }

  @Test
  void usersMeWithoutTokenReturnsUnauthorized() {
    webTestClient.get()
        .uri("/api/v1/users/me")
        .exchange()
        .expectStatus().isUnauthorized();
  }

  @Test
  void customerOrdersWithoutTokenReturnsUnauthorized() {
    webTestClient.get()
        .uri("/api/v1/customer/orders")
        .exchange()
        .expectStatus().isUnauthorized();
  }

  @Test
  void invalidBearerTokenReturnsUnauthorized() {
    webTestClient.get()
        .uri("/api/v1/users/me")
        .header(HttpHeaders.AUTHORIZATION, "Bearer invalid-token")
        .exchange()
        .expectStatus().isUnauthorized();
  }

  @Test
  void customerTokenCanAccessCustomerEndpoint() {
    String token = tokenForActiveUser(UserRole.CUSTOMER);

    webTestClient.get()
        .uri("/api/v1/customer/orders")
        .header(HttpHeaders.AUTHORIZATION, bearer(token))
        .exchange()
        .expectStatus().isOk();
  }

  @Test
  void customerTokenCannotAccessAdminEndpoint() {
    String token = tokenForActiveUser(UserRole.CUSTOMER);

    webTestClient.get()
        .uri("/api/v1/admin/users")
        .header(HttpHeaders.AUTHORIZATION, bearer(token))
        .exchange()
        .expectStatus().isForbidden();
  }

  @Test
  void sellerTokenCanAccessSellerEndpoint() {
    String token = tokenForActiveUser(UserRole.SELLER);

    webTestClient.get()
        .uri("/api/v1/seller/products")
        .header(HttpHeaders.AUTHORIZATION, bearer(token))
        .exchange()
        .expectStatus().isOk();
  }

  @Test
  void sellerTokenCannotAccessAdminEndpoint() {
    String token = tokenForActiveUser(UserRole.SELLER);

    webTestClient.get()
        .uri("/api/v1/admin/users")
        .header(HttpHeaders.AUTHORIZATION, bearer(token))
        .exchange()
        .expectStatus().isForbidden();
  }

  @Test
  void adminTokenCanAccessAdminEndpoint() {
    String token = tokenForActiveUser(UserRole.ADMIN);

    webTestClient.get()
        .uri("/api/v1/admin/users")
        .header(HttpHeaders.AUTHORIZATION, bearer(token))
        .exchange()
        .expectStatus().isOk();
  }

  @Test
  void blockedUserTokenReturnsUnauthorized() {
    String token = tokenForBlockedUser();

    webTestClient.get()
        .uri("/api/v1/users/me")
        .header(HttpHeaders.AUTHORIZATION, bearer(token))
        .exchange()
        .expectStatus().isUnauthorized();
  }

  @Test
  void usersMeReturnsCurrentUserProfile() {
    SavedUser savedUser = saveUser(UserRole.CUSTOMER);

    webTestClient.get()
        .uri("/api/v1/users/me")
        .header(HttpHeaders.AUTHORIZATION, bearer(jwtTokenProvider.generateAccessToken(savedUser.user())))
        .exchange()
        .expectStatus().isOk()
        .expectBody()
        .jsonPath("$.id").isEqualTo(savedUser.user().getId().toString())
        .jsonPath("$.email").isEqualTo(savedUser.user().getEmail())
        .jsonPath("$.role").isEqualTo("CUSTOMER")
        .jsonPath("$.status").isEqualTo("ACTIVE")
        .jsonPath("$.passwordHash").doesNotExist();
  }

  private String tokenForActiveUser(UserRole role) {
    return jwtTokenProvider.generateAccessToken(saveUser(role).user());
  }

  private String tokenForBlockedUser() {
    SavedUser savedUser = saveUser(UserRole.CUSTOMER);
    User user = savedUser.user();
    String token = jwtTokenProvider.generateAccessToken(user);
    user.block();

    StepVerifier.create(userRepository.save(user))
        .expectNextMatches(saved -> saved.getStatus() == user.getStatus())
        .verifyComplete();

    return token;
  }

  private SavedUser saveUser(UserRole role) {
    User user = User.createNewActiveUser(
        role.name().toLowerCase() + "-" + UUID.randomUUID() + "@example.com",
        passwordEncoder.encode("password123"),
        role
    );

    StepVerifier.create(userRepository.save(user))
        .expectNextMatches(saved -> saved.getId().equals(user.getId()))
        .verifyComplete();

    return new SavedUser(user);
  }

  private String bearer(String token) {
    return "Bearer " + token;
  }

  private record SavedUser(User user) {
  }

  @RestController
  @RequestMapping("/api/v1")
  static class TestRoleEndpoints {

    @GetMapping("/customer/orders")
    String customerOrders() {
      return "customer";
    }

    @GetMapping("/seller/products")
    String sellerProducts() {
      return "seller";
    }

    @GetMapping("/admin/users")
    String adminUsers() {
      return "admin";
    }
  }
}
