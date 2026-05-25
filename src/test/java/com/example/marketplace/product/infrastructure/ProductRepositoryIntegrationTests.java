package com.example.marketplace.product.infrastructure;

import com.example.marketplace.PostgresTestContainerConfig;
import com.example.marketplace.category.domain.Category;
import com.example.marketplace.category.domain.CategoryRepository;
import com.example.marketplace.product.domain.Product;
import com.example.marketplace.product.domain.ProductRepository;
import com.example.marketplace.product.domain.ProductStatus;
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
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SpringBootTest
class ProductRepositoryIntegrationTests {

  @DynamicPropertySource
  static void postgresProperties(DynamicPropertyRegistry registry) {
    PostgresTestContainerConfig.configure(registry);
  }

  @Autowired
  private ProductRepository productRepository;

  @Autowired
  private CategoryRepository categoryRepository;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private PasswordEncoder passwordEncoder;

  @Test
  void saveProduct() {
    Product product = newProduct();

    StepVerifier.create(productRepository.save(product))
        .assertNext(saved -> {
          assertThat(saved.getId()).isEqualTo(product.getId());
          assertThat(saved.getSellerId()).isEqualTo(product.getSellerId());
          assertThat(saved.getCategoryId()).isEqualTo(product.getCategoryId());
          assertThat(saved.getName()).isEqualTo(product.getName());
          assertThat(saved.getStatus()).isEqualTo(ProductStatus.DRAFT);
        })
        .verifyComplete();
  }

  @Test
  void findProductById() {
    Product product = newProduct();

    StepVerifier.create(productRepository.save(product).flatMap(saved -> productRepository.findById(saved.getId())))
        .assertNext(found -> {
          assertThat(found.getId()).isEqualTo(product.getId());
          assertThat(found.getName()).isEqualTo(product.getName());
        })
        .verifyComplete();
  }

  @Test
  void findAllActiveProducts() {
    Product activeProduct = newProduct();
    activeProduct.activate();
    Product inactiveProduct = newProduct();
    inactiveProduct.deactivate();

    StepVerifier.create(productRepository.save(activeProduct)
            .then(productRepository.save(inactiveProduct))
            .thenMany(productRepository.findAllActive().filter(product ->
                product.getId().equals(activeProduct.getId()) || product.getId().equals(inactiveProduct.getId())
            )))
        .assertNext(found -> {
          assertThat(found.getId()).isEqualTo(activeProduct.getId());
          assertThat(found.getStatus()).isEqualTo(ProductStatus.ACTIVE);
        })
        .verifyComplete();
  }

  @Test
  void findProductsBySellerId() {
    Product product = newProduct();

    StepVerifier.create(productRepository.save(product).thenMany(productRepository.findBySellerId(product.getSellerId())))
        .expectNextMatches(found -> found.getId().equals(product.getId()))
        .thenCancel()
        .verify();
  }

  private Product newProduct() {
    User seller = saveSeller();
    Category category = saveCategory();

    return Product.createNew(
        seller.getId(),
        category.getId(),
        "product-" + UUID.randomUUID(),
        "Repository product",
        BigDecimal.valueOf(19.99),
        7
    );
  }

  private User saveSeller() {
    User seller = User.createNewActiveUser(
        "seller-" + UUID.randomUUID() + "@example.com",
        passwordEncoder.encode("password123"),
        UserRole.SELLER
    );

    StepVerifier.create(userRepository.save(seller))
        .expectNextMatches(saved -> saved.getId().equals(seller.getId()))
        .verifyComplete();

    return seller;
  }

  private Category saveCategory() {
    Category category = Category.createNew("category-" + UUID.randomUUID(), "Product category");

    StepVerifier.create(categoryRepository.save(category))
        .expectNextMatches(saved -> saved.getId().equals(category.getId()))
        .verifyComplete();

    return category;
  }
}
