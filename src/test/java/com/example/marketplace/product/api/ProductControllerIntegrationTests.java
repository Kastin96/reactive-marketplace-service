package com.example.marketplace.product.api;

import com.example.marketplace.PostgresTestContainerConfig;
import com.example.marketplace.category.domain.Category;
import com.example.marketplace.category.domain.CategoryRepository;
import com.example.marketplace.product.domain.Product;
import com.example.marketplace.product.domain.ProductRepository;
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

import java.math.BigDecimal;
import java.util.UUID;

@ActiveProfiles("test")
@AutoConfigureWebTestClient
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ProductControllerIntegrationTests {

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
  private ProductRepository productRepository;

  @Autowired
  private PasswordEncoder passwordEncoder;

  @Autowired
  private JwtTokenProvider jwtTokenProvider;

  @Test
  void sellerCanCreateProduct() {
    Category category = saveCategory();
    CreateProductRequest request = createRequest(category.getId(), " Seller product ");
    User seller = saveUser(UserRole.SELLER);

    webTestClient.post()
        .uri("/api/v1/seller/products")
        .header(HttpHeaders.AUTHORIZATION, bearer(seller))
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(request)
        .exchange()
        .expectStatus().isCreated()
        .expectBody()
        .jsonPath("$.id").isNotEmpty()
        .jsonPath("$.sellerId").isEqualTo(seller.getId().toString())
        .jsonPath("$.categoryId").isEqualTo(category.getId().toString())
        .jsonPath("$.name").isEqualTo("Seller product")
        .jsonPath("$.status").isEqualTo("DRAFT");
  }

  @Test
  void customerCannotCreateProduct() {
    Category category = saveCategory();
    CreateProductRequest request = createRequest(category.getId(), "Customer product");

    webTestClient.post()
        .uri("/api/v1/seller/products")
        .header(HttpHeaders.AUTHORIZATION, bearer(saveUser(UserRole.CUSTOMER)))
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(request)
        .exchange()
        .expectStatus().isForbidden();
  }

  @Test
  void unauthenticatedUserCannotCreateProduct() {
    Category category = saveCategory();
    CreateProductRequest request = createRequest(category.getId(), "Anonymous product");

    webTestClient.post()
        .uri("/api/v1/seller/products")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(request)
        .exchange()
        .expectStatus().isUnauthorized();
  }

  @Test
  void authenticatedUserCanListActiveProducts() {
    Product activeProduct = saveProduct(true);
    Product inactiveProduct = saveProduct(false);

    webTestClient.get()
        .uri("/api/v1/products")
        .header(HttpHeaders.AUTHORIZATION, bearer(saveUser(UserRole.CUSTOMER)))
        .exchange()
        .expectStatus().isOk()
        .expectBody()
        .jsonPath("$[?(@.id == '%s')]".formatted(activeProduct.getId())).exists()
        .jsonPath("$[?(@.id == '%s')]".formatted(inactiveProduct.getId())).doesNotExist();
  }

  @Test
  void unauthenticatedUserCannotListProducts() {
    webTestClient.get()
        .uri("/api/v1/products")
        .exchange()
        .expectStatus().isUnauthorized();
  }

  @Test
  void adminCanActivateAndDeactivateProduct() {
    Product product = saveProduct(false);
    String adminToken = bearer(saveUser(UserRole.ADMIN));

    webTestClient.patch()
        .uri("/api/v1/admin/products/{productId}/activate", product.getId())
        .header(HttpHeaders.AUTHORIZATION, adminToken)
        .exchange()
        .expectStatus().isOk()
        .expectBody()
        .jsonPath("$.status").isEqualTo("ACTIVE");

    webTestClient.patch()
        .uri("/api/v1/admin/products/{productId}/deactivate", product.getId())
        .header(HttpHeaders.AUTHORIZATION, adminToken)
        .exchange()
        .expectStatus().isOk()
        .expectBody()
        .jsonPath("$.status").isEqualTo("INACTIVE");
  }

  @Test
  void sellerCannotActivateProductThroughAdminEndpoint() {
    Product product = saveProduct(false);

    webTestClient.patch()
        .uri("/api/v1/admin/products/{productId}/activate", product.getId())
        .header(HttpHeaders.AUTHORIZATION, bearer(saveUser(UserRole.SELLER)))
        .exchange()
        .expectStatus().isForbidden();
  }

  @Test
  void sellerCannotDeactivateProductThroughAdminEndpoint() {
    Product product = saveProduct(true);

    webTestClient.patch()
        .uri("/api/v1/admin/products/{productId}/deactivate", product.getId())
        .header(HttpHeaders.AUTHORIZATION, bearer(saveUser(UserRole.SELLER)))
        .exchange()
        .expectStatus().isForbidden();
  }

  @Test
  void invalidProductRequestReturnsValidationError() {
    Category category = saveCategory();
    CreateProductRequest request = new CreateProductRequest(
        category.getId(),
        " ",
        null,
        BigDecimal.ZERO,
        -1
    );

    webTestClient.post()
        .uri("/api/v1/seller/products")
        .header(HttpHeaders.AUTHORIZATION, bearer(saveUser(UserRole.SELLER)))
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(request)
        .exchange()
        .expectStatus().isBadRequest();
  }

  private Product saveProduct(boolean active) {
    User seller = saveUser(UserRole.SELLER);
    Category category = saveCategory();
    Product product = Product.createNew(
        seller.getId(),
        category.getId(),
        "product-" + UUID.randomUUID(),
        "Controller product",
        BigDecimal.valueOf(29.99),
        6
    );

    if (active) {
      product.activate();
    } else {
      product.deactivate();
    }

    StepVerifier.create(productRepository.save(product))
        .expectNextMatches(saved -> saved.getId().equals(product.getId()))
        .verifyComplete();

    return product;
  }

  private Category saveCategory() {
    Category category = Category.createNew("category-" + UUID.randomUUID(), "Product category");

    StepVerifier.create(categoryRepository.save(category))
        .expectNextMatches(saved -> saved.getId().equals(category.getId()))
        .verifyComplete();

    return category;
  }

  private User saveUser(UserRole role) {
    User user = User.createNewActiveUser(
        role.name().toLowerCase() + "-" + UUID.randomUUID() + "@example.com",
        passwordEncoder.encode("password123"),
        role
    );

    StepVerifier.create(userRepository.save(user))
        .expectNextMatches(saved -> saved.getId().equals(user.getId()))
        .verifyComplete();

    return user;
  }

  private String bearer(User user) {
    return "Bearer " + jwtTokenProvider.generateAccessToken(user);
  }

  private CreateProductRequest createRequest(UUID categoryId, String name) {
    return new CreateProductRequest(
        categoryId,
        name,
        "Product description",
        BigDecimal.valueOf(29.99),
        6
    );
  }
}
