package com.example.marketplace.order.api;

import com.example.marketplace.PostgresTestContainerConfig;
import com.example.marketplace.category.domain.Category;
import com.example.marketplace.category.domain.CategoryRepository;
import com.example.marketplace.order.domain.Order;
import com.example.marketplace.order.domain.OrderItem;
import com.example.marketplace.order.domain.OrderRepository;
import com.example.marketplace.order.domain.OrderStatus;
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
import java.util.List;
import java.util.UUID;

@ActiveProfiles("test")
@AutoConfigureWebTestClient
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class OrderControllerIntegrationTests {

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
  private OrderRepository orderRepository;

  @Autowired
  private PasswordEncoder passwordEncoder;

  @Autowired
  private JwtTokenProvider jwtTokenProvider;

  @Test
  void customerCanCreateOrder() {
    User customer = saveUser(UserRole.CUSTOMER);
    Product product = saveProduct(saveUser(UserRole.SELLER), true, 5);
    CreateOrderRequest request = new CreateOrderRequest(List.of(new CreateOrderItemRequest(product.getId(), 2)));

    webTestClient.post()
        .uri("/api/v1/customer/orders")
        .header(HttpHeaders.AUTHORIZATION, bearer(customer))
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(request)
        .exchange()
        .expectStatus().isCreated()
        .expectBody()
        .jsonPath("$.customerId").isEqualTo(customer.getId().toString())
        .jsonPath("$.status").isEqualTo("CREATED")
        .jsonPath("$.totalAmount").isEqualTo(59.98)
        .jsonPath("$.items[0].productId").isEqualTo(product.getId().toString())
        .jsonPath("$.items[0].unitPrice").isEqualTo(29.99)
        .jsonPath("$.items[0].productName").isEqualTo(product.getName());
  }

  @Test
  void sellerCannotCreateCustomerOrder() {
    Product product = saveProduct(saveUser(UserRole.SELLER), true, 5);
    CreateOrderRequest request = new CreateOrderRequest(List.of(new CreateOrderItemRequest(product.getId(), 1)));

    webTestClient.post()
        .uri("/api/v1/customer/orders")
        .header(HttpHeaders.AUTHORIZATION, bearer(saveUser(UserRole.SELLER)))
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(request)
        .exchange()
        .expectStatus().isForbidden();
  }

  @Test
  void unauthenticatedUserCannotCreateOrder() {
    Product product = saveProduct(saveUser(UserRole.SELLER), true, 5);
    CreateOrderRequest request = new CreateOrderRequest(List.of(new CreateOrderItemRequest(product.getId(), 1)));

    webTestClient.post()
        .uri("/api/v1/customer/orders")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(request)
        .exchange()
        .expectStatus().isUnauthorized();
  }

  @Test
  void customerCanListOwnOrders() {
    User customer = saveUser(UserRole.CUSTOMER);
    Order order = saveOrder(customer, saveUser(UserRole.SELLER));

    webTestClient.get()
        .uri("/api/v1/customer/orders?page=0&size=20")
        .header(HttpHeaders.AUTHORIZATION, bearer(customer))
        .exchange()
        .expectStatus().isOk()
        .expectBody()
        .jsonPath("$.content[?(@.id == '%s')]".formatted(order.getId())).exists()
        .jsonPath("$.page").isEqualTo(0)
        .jsonPath("$.size").isEqualTo(20)
        .jsonPath("$.totalElements").isEqualTo(1)
        .jsonPath("$.totalPages").isEqualTo(1)
        .jsonPath("$.last").isEqualTo(true);
  }

  @Test
  void sellerCanListSellerOrders() {
    User customer = saveUser(UserRole.CUSTOMER);
    User seller = saveUser(UserRole.SELLER);
    Order order = saveOrder(customer, seller);

    webTestClient.get()
        .uri("/api/v1/seller/orders?page=0&size=20")
        .header(HttpHeaders.AUTHORIZATION, bearer(seller))
        .exchange()
        .expectStatus().isOk()
        .expectBody()
        .jsonPath("$.content[?(@.id == '%s')]".formatted(order.getId())).exists()
        .jsonPath("$.content[0].items[0].sellerId").isEqualTo(seller.getId().toString())
        .jsonPath("$.totalElements").isEqualTo(1);
  }

  @Test
  void adminCanListAllOrders() {
    Order order = saveOrder(saveUser(UserRole.CUSTOMER), saveUser(UserRole.SELLER));

    webTestClient.get()
        .uri("/api/v1/admin/orders?page=0&size=20")
        .header(HttpHeaders.AUTHORIZATION, bearer(saveUser(UserRole.ADMIN)))
        .exchange()
        .expectStatus().isOk()
        .expectBody()
        .jsonPath("$.content[?(@.id == '%s')]".formatted(order.getId())).exists()
        .jsonPath("$.page").isEqualTo(0)
        .jsonPath("$.size").isEqualTo(20)
        .jsonPath("$.totalElements").isNumber();
  }

  @Test
  void adminCanUpdateOrderStatus() {
    Order order = saveOrder(saveUser(UserRole.CUSTOMER), saveUser(UserRole.SELLER));

    webTestClient.patch()
        .uri("/api/v1/admin/orders/{orderId}/status", order.getId())
        .header(HttpHeaders.AUTHORIZATION, bearer(saveUser(UserRole.ADMIN)))
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(new UpdateOrderStatusRequest(OrderStatus.PROCESSING))
        .exchange()
        .expectStatus().isOk()
        .expectBody()
        .jsonPath("$.status").isEqualTo("PROCESSING");
  }

  @Test
  void customerCannotAccessAdminOrderEndpoints() {
    webTestClient.get()
        .uri("/api/v1/admin/orders")
        .header(HttpHeaders.AUTHORIZATION, bearer(saveUser(UserRole.CUSTOMER)))
        .exchange()
        .expectStatus().isForbidden();
  }

  @Test
  void invalidOrderRequestReturnsValidationError() {
    CreateOrderRequest request = new CreateOrderRequest(List.of(new CreateOrderItemRequest(null, 0)));

    webTestClient.post()
        .uri("/api/v1/customer/orders")
        .header(HttpHeaders.AUTHORIZATION, bearer(saveUser(UserRole.CUSTOMER)))
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(request)
        .exchange()
        .expectStatus().isBadRequest();
  }

  private Order saveOrder(User customer, User seller) {
    Product product = saveProduct(seller, true, 10);
    UUID orderId = UUID.randomUUID();
    OrderItem item = OrderItem.createNew(
        orderId,
        product.getId(),
        product.getSellerId(),
        product.getName(),
        product.getPrice(),
        1
    );
    Order order = Order.createNew(customer.getId(), List.of(item));

    StepVerifier.create(orderRepository.save(order))
        .expectNextMatches(saved -> saved.getId().equals(order.getId()))
        .verifyComplete();

    return order;
  }

  private Product saveProduct(User seller, boolean active, int stockQuantity) {
    Category category = Category.createNew("order-api-category-" + UUID.randomUUID(), "Order API category");

    StepVerifier.create(categoryRepository.save(category))
        .expectNextMatches(saved -> saved.getId().equals(category.getId()))
        .verifyComplete();

    Product product = Product.createNew(
        seller.getId(),
        category.getId(),
        "order-api-product-" + UUID.randomUUID(),
        "Order API product",
        BigDecimal.valueOf(29.99),
        stockQuantity
    );

    if (active) {
      product.activate();
    }

    StepVerifier.create(productRepository.save(product))
        .expectNextMatches(saved -> saved.getId().equals(product.getId()))
        .verifyComplete();

    return product;
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
}
