package com.example.marketplace.category.api;

import com.example.marketplace.PostgresTestContainerConfig;
import com.example.marketplace.category.domain.Category;
import com.example.marketplace.category.domain.CategoryRepository;
import com.example.marketplace.security.JwtTokenProvider;
import com.example.marketplace.user.domain.User;
import com.example.marketplace.user.domain.UserRepository;
import com.example.marketplace.user.domain.UserRole;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
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
class CategoryControllerIntegrationTests {

  @DynamicPropertySource
  static void postgresProperties(DynamicPropertyRegistry registry) {
    PostgresTestContainerConfig.configure(registry);
  }

  @Autowired
  private WebTestClient webTestClient;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private CategoryRepository categoryRepository;

  @Autowired
  private PasswordEncoder passwordEncoder;

  @Autowired
  private JwtTokenProvider jwtTokenProvider;

  @Test
  void adminCanCreateCategory() {
    CreateCategoryRequest request = new CreateCategoryRequest(uniqueName("admin-create"), "Created by admin");

    webTestClient.post()
        .uri("/api/v1/admin/categories")
        .header(HttpHeaders.AUTHORIZATION, bearerTokenFor(UserRole.ADMIN))
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(request)
        .exchange()
        .expectStatus().isCreated()
        .expectBody()
        .jsonPath("$.id").isNotEmpty()
        .jsonPath("$.name").isEqualTo(request.name())
        .jsonPath("$.description").isEqualTo(request.description())
        .jsonPath("$.createdAt").isNotEmpty()
        .jsonPath("$.updatedAt").isNotEmpty();
  }

  @Test
  void customerCannotCreateCategory() {
    CreateCategoryRequest request = new CreateCategoryRequest(uniqueName("customer-create"), null);

    webTestClient.post()
        .uri("/api/v1/admin/categories")
        .header(HttpHeaders.AUTHORIZATION, bearerTokenFor(UserRole.CUSTOMER))
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(request)
        .exchange()
        .expectStatus().isForbidden();
  }

  @Test
  void unauthenticatedUserCannotCreateCategory() {
    CreateCategoryRequest request = new CreateCategoryRequest(uniqueName("anonymous-create"), null);

    webTestClient.post()
        .uri("/api/v1/admin/categories")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(request)
        .exchange()
        .expectStatus().isUnauthorized();
  }

  @Test
  void authenticatedUserCanListCategories() {
    Category category = saveCategory("list");

    webTestClient.get()
        .uri("/api/v1/categories")
        .header(HttpHeaders.AUTHORIZATION, bearerTokenFor(UserRole.CUSTOMER))
        .exchange()
        .expectStatus().isOk()
        .expectBody()
        .jsonPath("$[?(@.id == '%s')]".formatted(category.getId())).exists();
  }

  @Test
  void unauthenticatedUserCannotListCategories() {
    webTestClient.get()
        .uri("/api/v1/categories")
        .exchange()
        .expectStatus().isUnauthorized();
  }

  @Test
  void invalidCategoryRequestReturnsValidationError() {
    CreateCategoryRequest request = new CreateCategoryRequest(" ", null);

    webTestClient.post()
        .uri("/api/v1/admin/categories")
        .header(HttpHeaders.AUTHORIZATION, bearerTokenFor(UserRole.ADMIN))
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(request)
        .exchange()
        .expectStatus().isBadRequest()
        .expectBody()
        .jsonPath("$.status").isEqualTo(400)
        .jsonPath("$.error").isEqualTo("Bad Request")
        .jsonPath("$.message").isEqualTo("Validation failed")
        .jsonPath("$.path").isEqualTo("/api/v1/admin/categories")
        .jsonPath("$.fieldErrors[?(@.field == 'name')]").exists();
  }

  private Category saveCategory(String prefix) {
    Category category = Category.createNew(uniqueName(prefix), "Saved category");

    StepVerifier.create(categoryRepository.save(category))
        .expectNextMatches(saved -> saved.getId().equals(category.getId()))
        .verifyComplete();

    return category;
  }

  private String bearerTokenFor(UserRole role) {
    User user = User.createNewActiveUser(
        role.name().toLowerCase() + "-" + UUID.randomUUID() + "@example.com",
        passwordEncoder.encode("password123"),
        role
    );

    StepVerifier.create(userRepository.save(user))
        .expectNextMatches(saved -> saved.getId().equals(user.getId()))
        .verifyComplete();

    return "Bearer " + jwtTokenProvider.generateAccessToken(user);
  }

  private String uniqueName(String prefix) {
    return prefix + "-" + UUID.randomUUID();
  }
}
