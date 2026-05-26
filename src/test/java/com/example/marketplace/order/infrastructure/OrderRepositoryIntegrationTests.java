package com.example.marketplace.order.infrastructure;

import com.example.marketplace.PostgresTestContainerConfig;
import com.example.marketplace.category.domain.Category;
import com.example.marketplace.category.domain.CategoryRepository;
import com.example.marketplace.common.pagination.PageRequest;
import com.example.marketplace.order.domain.Order;
import com.example.marketplace.order.domain.OrderItem;
import com.example.marketplace.order.domain.OrderRepository;
import com.example.marketplace.product.domain.Product;
import com.example.marketplace.product.domain.ProductRepository;
import com.example.marketplace.user.domain.User;
import com.example.marketplace.user.domain.UserRepository;
import com.example.marketplace.user.domain.UserRole;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SpringBootTest
class OrderRepositoryIntegrationTests {

  @DynamicPropertySource
  static void postgresProperties(DynamicPropertyRegistry registry) {
    PostgresTestContainerConfig.configure(registry);
  }

  @Autowired
  private OrderRepository orderRepository;

  @Autowired
  private ProductRepository productRepository;

  @Autowired
  private CategoryRepository categoryRepository;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private PasswordEncoder passwordEncoder;

  @Test
  void saveOrderWithItems() {
    Order order = newOrder();

    StepVerifier.create(orderRepository.save(order))
        .assertNext(saved -> {
          assertThat(saved.getId()).isEqualTo(order.getId());
          assertThat(saved.getItems()).hasSize(1);
          assertThat(saved.getTotalAmount()).isEqualByComparingTo(order.getTotalAmount());
        })
        .verifyComplete();
  }

  @Test
  void findOrderById() {
    Order order = newOrder();

    StepVerifier.create(orderRepository.save(order).flatMap(saved -> orderRepository.findById(saved.getId())))
        .assertNext(found -> {
          assertThat(found.getId()).isEqualTo(order.getId());
          assertThat(found.getItems()).hasSize(1);
        })
        .verifyComplete();
  }

  @Test
  void findOrdersByCustomerId() {
    Order order = newOrder();

    StepVerifier.create(orderRepository.save(order)
            .thenMany(orderRepository.findByCustomerId(order.getCustomerId(), PageRequest.of(0, 20))))
        .expectNextMatches(found -> found.getId().equals(order.getId()))
        .thenCancel()
        .verify();
  }

  @Test
  void findOrdersBySellerId() {
    Order order = newOrder();
    UUID sellerId = order.getItems().getFirst().sellerId();

    StepVerifier.create(orderRepository.save(order)
            .thenMany(orderRepository.findBySellerId(sellerId, PageRequest.of(0, 20))))
        .expectNextMatches(found -> found.getId().equals(order.getId()))
        .thenCancel()
        .verify();
  }

  @Test
  void findAllOrders() {
    Order order = newOrder();

    StepVerifier.create(orderRepository.save(order)
            .thenMany(orderRepository.findAll(PageRequest.of(0, 20)).filter(found -> found.getId().equals(order.getId()))))
        .expectNextMatches(found -> found.getItems().size() == 1)
        .verifyComplete();
  }

  @Test
  void countOrdersByCustomerAndSeller() {
    Order order = newOrder();
    UUID sellerId = order.getItems().getFirst().sellerId();

    StepVerifier.create(orderRepository.save(order)
            .then(Mono.zip(
                orderRepository.countByCustomerId(order.getCustomerId()),
                orderRepository.countBySellerId(sellerId),
                orderRepository.countAll()
            )))
        .assertNext(result -> {
          assertThat(result.getT1()).isGreaterThanOrEqualTo(1);
          assertThat(result.getT2()).isGreaterThanOrEqualTo(1);
          assertThat(result.getT3()).isGreaterThanOrEqualTo(1);
        })
        .verifyComplete();
  }

  private Order newOrder() {
    User customer = saveUser(UserRole.CUSTOMER);
    Product product = saveProduct();
    UUID orderId = UUID.randomUUID();
    OrderItem item = OrderItem.createNew(
        orderId,
        product.getId(),
        product.getSellerId(),
        product.getName(),
        product.getPrice(),
        2
    );

    return Order.createNew(customer.getId(), List.of(item));
  }

  private Product saveProduct() {
    User seller = saveUser(UserRole.SELLER);
    Category category = Category.createNew("order-category-" + UUID.randomUUID(), "Order category");

    StepVerifier.create(categoryRepository.save(category))
        .expectNextMatches(saved -> saved.getId().equals(category.getId()))
        .verifyComplete();

    Product product = Product.createNew(
        seller.getId(),
        category.getId(),
        "order-product-" + UUID.randomUUID(),
        "Order product",
        BigDecimal.valueOf(15.00),
        10
    );
    product.activate();

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
}
