package com.example.marketplace.product.application;

import com.example.marketplace.category.domain.Category;
import com.example.marketplace.category.domain.CategoryRepository;
import com.example.marketplace.common.exception.CategoryNotFoundException;
import com.example.marketplace.common.exception.ProductAccessDeniedException;
import com.example.marketplace.common.pagination.PageRequest;
import com.example.marketplace.product.api.CreateProductRequest;
import com.example.marketplace.product.api.UpdateProductRequest;
import com.example.marketplace.product.domain.Product;
import com.example.marketplace.product.domain.ProductRepository;
import com.example.marketplace.product.domain.ProductStatus;
import com.example.marketplace.security.CurrentUserProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTests {

  @Mock
  private ProductRepository productRepository;

  @Mock
  private CategoryRepository categoryRepository;

  @Mock
  private CurrentUserProvider currentUserProvider;

  private ProductService productService;

  @BeforeEach
  void setUp() {
    productService = new ProductService(productRepository, categoryRepository, currentUserProvider);
  }

  @Test
  void sellerCreatesProductSuccessfully() {
    UUID sellerId = UUID.randomUUID();
    Category category = Category.createNew("Electronics", null);
    CreateProductRequest request = createRequest(category.getId(), " Phone ");
    when(categoryRepository.findById(category.getId())).thenReturn(Mono.just(category));
    when(currentUserProvider.currentUserId()).thenReturn(Mono.just(sellerId));
    when(productRepository.save(any(Product.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

    StepVerifier.create(productService.createProduct(request))
        .assertNext(response -> {
          assertThat(response.sellerId()).isEqualTo(sellerId);
          assertThat(response.categoryId()).isEqualTo(category.getId());
          assertThat(response.name()).isEqualTo("Phone");
          assertThat(response.status()).isEqualTo(ProductStatus.DRAFT);
        })
        .verifyComplete();

    ArgumentCaptor<Product> productCaptor = ArgumentCaptor.forClass(Product.class);
    verify(productRepository).save(productCaptor.capture());
    assertThat(productCaptor.getValue().getSellerId()).isEqualTo(sellerId);
  }

  @Test
  void productCreationFailsWhenCategoryDoesNotExist() {
    UUID categoryId = UUID.randomUUID();
    when(categoryRepository.findById(categoryId)).thenReturn(Mono.empty());

    StepVerifier.create(productService.createProduct(createRequest(categoryId, "Phone")))
        .expectError(CategoryNotFoundException.class)
        .verify();
  }

  @Test
  void sellerUpdatesOwnProductSuccessfully() {
    UUID sellerId = UUID.randomUUID();
    Category oldCategory = Category.createNew("Old", null);
    Category newCategory = Category.createNew("New", null);
    Product product = Product.createNew(
        sellerId,
        oldCategory.getId(),
        "Old phone",
        null,
        BigDecimal.valueOf(10),
        5
    );
    UpdateProductRequest request = updateRequest(newCategory.getId(), " New phone ");
    when(categoryRepository.findById(newCategory.getId())).thenReturn(Mono.just(newCategory));
    when(currentUserProvider.currentUserId()).thenReturn(Mono.just(sellerId));
    when(productRepository.findById(product.getId())).thenReturn(Mono.just(product));
    when(productRepository.save(product)).thenReturn(Mono.just(product));

    StepVerifier.create(productService.updateOwnProduct(product.getId(), request))
        .assertNext(response -> {
          assertThat(response.categoryId()).isEqualTo(newCategory.getId());
          assertThat(response.name()).isEqualTo("New phone");
          assertThat(response.price()).isEqualByComparingTo("49.99");
          assertThat(response.stockQuantity()).isEqualTo(12);
        })
        .verifyComplete();
  }

  @Test
  void sellerCannotUpdateAnotherSellersProduct() {
    UUID ownerId = UUID.randomUUID();
    UUID currentSellerId = UUID.randomUUID();
    Category category = Category.createNew("Electronics", null);
    Product product = Product.createNew(
        ownerId,
        category.getId(),
        "Phone",
        null,
        BigDecimal.valueOf(10),
        5
    );
    when(categoryRepository.findById(category.getId())).thenReturn(Mono.just(category));
    when(currentUserProvider.currentUserId()).thenReturn(Mono.just(currentSellerId));
    when(productRepository.findById(product.getId())).thenReturn(Mono.just(product));

    StepVerifier.create(productService.updateOwnProduct(product.getId(), updateRequest(category.getId(), "Phone")))
        .expectError(ProductAccessDeniedException.class)
        .verify();
  }

  @Test
  void adminActivatesProduct() {
    Product product = draftProduct();
    when(productRepository.findById(product.getId())).thenReturn(Mono.just(product));
    when(productRepository.save(product)).thenReturn(Mono.just(product));

    StepVerifier.create(productService.activateProduct(product.getId()))
        .assertNext(response -> assertThat(response.status()).isEqualTo(ProductStatus.ACTIVE))
        .verifyComplete();
  }

  @Test
  void adminDeactivatesProduct() {
    Product product = draftProduct();
    product.activate();
    when(productRepository.findById(product.getId())).thenReturn(Mono.just(product));
    when(productRepository.save(product)).thenReturn(Mono.just(product));

    StepVerifier.create(productService.deactivateProduct(product.getId()))
        .assertNext(response -> assertThat(response.status()).isEqualTo(ProductStatus.INACTIVE))
        .verifyComplete();
  }

  @Test
  void activeProductListingReturnsActiveProducts() {
    Product activeProduct = draftProduct();
    activeProduct.activate();
    PageRequest pageRequest = PageRequest.of(0, 20);
    when(productRepository.findAllActive(pageRequest)).thenReturn(Flux.just(activeProduct));
    when(productRepository.countAllActive()).thenReturn(Mono.just(1L));

    StepVerifier.create(productService.getActiveProducts(pageRequest))
        .assertNext(response -> {
          assertThat(response.content()).hasSize(1);
          assertThat(response.content().getFirst().status()).isEqualTo(ProductStatus.ACTIVE);
          assertThat(response.page()).isZero();
          assertThat(response.size()).isEqualTo(20);
          assertThat(response.totalElements()).isEqualTo(1);
          assertThat(response.totalPages()).isEqualTo(1);
          assertThat(response.last()).isTrue();
        })
        .verifyComplete();
  }

  private Product draftProduct() {
    return Product.createNew(
        UUID.randomUUID(),
        UUID.randomUUID(),
        "Phone",
        null,
        BigDecimal.valueOf(10),
        5
    );
  }

  private CreateProductRequest createRequest(UUID categoryId, String name) {
    return new CreateProductRequest(
        categoryId,
        name,
        "Smartphone",
        BigDecimal.valueOf(49.99),
        12
    );
  }

  private UpdateProductRequest updateRequest(UUID categoryId, String name) {
    return new UpdateProductRequest(
        categoryId,
        name,
        "Updated smartphone",
        BigDecimal.valueOf(49.99),
        12
    );
  }
}
