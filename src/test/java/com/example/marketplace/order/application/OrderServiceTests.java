package com.example.marketplace.order.application;

import com.example.marketplace.common.exception.InactiveProductOrderException;
import com.example.marketplace.common.exception.InsufficientStockException;
import com.example.marketplace.common.exception.InvalidOrderStatusTransitionException;
import com.example.marketplace.common.exception.OrderAccessDeniedException;
import com.example.marketplace.common.exception.ProductNotFoundException;
import com.example.marketplace.common.pagination.PageRequest;
import com.example.marketplace.order.api.CreateOrderItemRequest;
import com.example.marketplace.order.api.CreateOrderRequest;
import com.example.marketplace.order.api.UpdateOrderStatusRequest;
import com.example.marketplace.order.domain.Order;
import com.example.marketplace.order.domain.OrderItem;
import com.example.marketplace.order.domain.OrderRepository;
import com.example.marketplace.order.domain.OrderStatus;
import com.example.marketplace.product.domain.Product;
import com.example.marketplace.product.domain.ProductRepository;
import com.example.marketplace.security.CurrentUserProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTests {

  @Mock
  private OrderRepository orderRepository;

  @Mock
  private ProductRepository productRepository;

  @Mock
  private CurrentUserProvider currentUserProvider;

  private OrderService orderService;

  @BeforeEach
  void setUp() {
    orderService = new OrderService(orderRepository, productRepository, currentUserProvider);
  }

  @Test
  void customerCreatesOrderSuccessfully() {
    UUID customerId = UUID.randomUUID();
    Product product = activeProduct(BigDecimal.valueOf(25), 10);
    when(currentUserProvider.currentUserId()).thenReturn(Mono.just(customerId));
    when(productRepository.findById(product.getId())).thenReturn(Mono.just(product));
    when(productRepository.decreaseStockIfAvailable(product.getId(), 2)).thenReturn(Mono.just(product));
    when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

    StepVerifier.create(orderService.createOrder(createRequest(product.getId(), 2)))
        .assertNext(response -> {
          assertThat(response.customerId()).isEqualTo(customerId);
          assertThat(response.status()).isEqualTo(OrderStatus.CREATED);
          assertThat(response.totalAmount()).isEqualByComparingTo("50");
          assertThat(response.items()).hasSize(1);
          assertThat(response.items().getFirst().productId()).isEqualTo(product.getId());
        })
        .verifyComplete();
  }

  @Test
  void orderCreationFailsWhenProductDoesNotExist() {
    UUID productId = UUID.randomUUID();
    when(currentUserProvider.currentUserId()).thenReturn(Mono.just(UUID.randomUUID()));
    when(productRepository.findById(productId)).thenReturn(Mono.empty());

    StepVerifier.create(orderService.createOrder(createRequest(productId, 1)))
        .expectError(ProductNotFoundException.class)
        .verify();
  }

  @Test
  void orderCreationFailsWhenProductIsInactive() {
    Product product = activeProduct(BigDecimal.TEN, 5);
    product.deactivate();
    when(currentUserProvider.currentUserId()).thenReturn(Mono.just(UUID.randomUUID()));
    when(productRepository.findById(product.getId())).thenReturn(Mono.just(product));

    StepVerifier.create(orderService.createOrder(createRequest(product.getId(), 1)))
        .expectError(InactiveProductOrderException.class)
        .verify();
  }

  @Test
  void orderCreationFailsWhenStockIsInsufficient() {
    Product product = activeProduct(BigDecimal.TEN, 1);
    when(currentUserProvider.currentUserId()).thenReturn(Mono.just(UUID.randomUUID()));
    when(productRepository.findById(product.getId())).thenReturn(Mono.just(product));

    StepVerifier.create(orderService.createOrder(createRequest(product.getId(), 2)))
        .expectError(InsufficientStockException.class)
        .verify();
  }

  @Test
  void orderTotalIsCalculatedOnBackend() {
    Product product = activeProduct(BigDecimal.valueOf(12.50), 10);
    when(currentUserProvider.currentUserId()).thenReturn(Mono.just(UUID.randomUUID()));
    when(productRepository.findById(product.getId())).thenReturn(Mono.just(product));
    when(productRepository.decreaseStockIfAvailable(product.getId(), 3)).thenReturn(Mono.just(product));
    when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

    StepVerifier.create(orderService.createOrder(createRequest(product.getId(), 3)))
        .assertNext(response -> assertThat(response.totalAmount()).isEqualByComparingTo("37.50"))
        .verifyComplete();
  }

  @Test
  void orderItemStoresProductPriceSnapshot() {
    Product product = activeProduct(BigDecimal.valueOf(19.99), 5);
    when(currentUserProvider.currentUserId()).thenReturn(Mono.just(UUID.randomUUID()));
    when(productRepository.findById(product.getId())).thenReturn(Mono.just(product));
    when(productRepository.decreaseStockIfAvailable(product.getId(), 1)).thenReturn(Mono.just(product));
    when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

    StepVerifier.create(orderService.createOrder(createRequest(product.getId(), 1)))
        .assertNext(response -> assertThat(response.items().getFirst().unitPrice()).isEqualByComparingTo("19.99"))
        .verifyComplete();
  }

  @Test
  void orderItemStoresProductNameSnapshot() {
    Product product = activeProduct(BigDecimal.TEN, 5);
    when(currentUserProvider.currentUserId()).thenReturn(Mono.just(UUID.randomUUID()));
    when(productRepository.findById(product.getId())).thenReturn(Mono.just(product));
    when(productRepository.decreaseStockIfAvailable(product.getId(), 1)).thenReturn(Mono.just(product));
    when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

    StepVerifier.create(orderService.createOrder(createRequest(product.getId(), 1)))
        .assertNext(response -> assertThat(response.items().getFirst().productName()).isEqualTo(product.getName()))
        .verifyComplete();
  }

  @Test
  void orderCreationFailsWhenAtomicStockDecreaseFails() {
    Product product = activeProduct(BigDecimal.TEN, 2);
    when(currentUserProvider.currentUserId()).thenReturn(Mono.just(UUID.randomUUID()));
    when(productRepository.findById(product.getId())).thenReturn(Mono.just(product));
    when(productRepository.decreaseStockIfAvailable(product.getId(), 2)).thenReturn(Mono.empty());

    StepVerifier.create(orderService.createOrder(createRequest(product.getId(), 2)))
        .expectError(InsufficientStockException.class)
        .verify();

    verify(orderRepository, never()).save(any(Order.class));
  }

  @Test
  void customerCanViewOwnOrder() {
    UUID customerId = UUID.randomUUID();
    Order order = orderForCustomer(customerId, UUID.randomUUID());
    when(currentUserProvider.currentUserId()).thenReturn(Mono.just(customerId));
    when(orderRepository.findById(order.getId())).thenReturn(Mono.just(order));

    StepVerifier.create(orderService.getCurrentCustomerOrderById(order.getId()))
        .assertNext(response -> assertThat(response.id()).isEqualTo(order.getId()))
        .verifyComplete();
  }

  @Test
  void customerCannotViewAnotherCustomersOrder() {
    Order order = orderForCustomer(UUID.randomUUID(), UUID.randomUUID());
    when(currentUserProvider.currentUserId()).thenReturn(Mono.just(UUID.randomUUID()));
    when(orderRepository.findById(order.getId())).thenReturn(Mono.just(order));

    StepVerifier.create(orderService.getCurrentCustomerOrderById(order.getId()))
        .expectError(OrderAccessDeniedException.class)
        .verify();
  }

  @Test
  void sellerSeesOnlyOrdersContainingTheirProducts() {
    UUID sellerId = UUID.randomUUID();
    UUID otherSellerId = UUID.randomUUID();
    Order order = orderForCustomer(UUID.randomUUID(), sellerId, otherSellerId);
    PageRequest pageRequest = PageRequest.of(0, 20);
    when(currentUserProvider.currentUserId()).thenReturn(Mono.just(sellerId));
    when(orderRepository.findBySellerId(sellerId, pageRequest)).thenReturn(Flux.just(order));
    when(orderRepository.countBySellerId(sellerId)).thenReturn(Mono.just(1L));

    StepVerifier.create(orderService.getCurrentSellerOrders(pageRequest))
        .assertNext(response -> {
          assertThat(response.content()).hasSize(1);
          assertThat(response.content().getFirst().items()).hasSize(1);
          assertThat(response.content().getFirst().items().getFirst().sellerId()).isEqualTo(sellerId);
          assertThat(response.totalElements()).isEqualTo(1);
          assertThat(response.last()).isTrue();
        })
        .verifyComplete();
  }

  @Test
  void adminUpdatesOrderStatusSuccessfully() {
    Order order = orderForCustomer(UUID.randomUUID(), UUID.randomUUID());
    when(orderRepository.findById(order.getId())).thenReturn(Mono.just(order));
    when(orderRepository.save(order)).thenReturn(Mono.just(order));

    StepVerifier.create(orderService.updateOrderStatus(
            order.getId(),
            new UpdateOrderStatusRequest(OrderStatus.PROCESSING)
        ))
        .assertNext(response -> assertThat(response.status()).isEqualTo(OrderStatus.PROCESSING))
        .verifyComplete();
  }

  @Test
  void invalidStatusTransitionIsRejected() {
    Order order = orderForCustomer(UUID.randomUUID(), UUID.randomUUID());
    when(orderRepository.findById(order.getId())).thenReturn(Mono.just(order));

    StepVerifier.create(orderService.updateOrderStatus(
            order.getId(),
            new UpdateOrderStatusRequest(OrderStatus.DELIVERED)
        ))
        .expectError(InvalidOrderStatusTransitionException.class)
        .verify();
  }

  @Test
  void customerCannotCancelOrderAfterShipped() {
    UUID customerId = UUID.randomUUID();
    Order order = orderForCustomer(customerId, UUID.randomUUID());
    order.changeStatus(OrderStatus.PROCESSING);
    order.changeStatus(OrderStatus.SHIPPED);
    when(currentUserProvider.currentUserId()).thenReturn(Mono.just(customerId));
    when(orderRepository.findById(order.getId())).thenReturn(Mono.just(order));

    StepVerifier.create(orderService.cancelCurrentCustomerOrder(order.getId()))
        .expectError(InvalidOrderStatusTransitionException.class)
        .verify();
  }

  private CreateOrderRequest createRequest(UUID productId, int quantity) {
    return new CreateOrderRequest(List.of(new CreateOrderItemRequest(productId, quantity)));
  }

  private Product activeProduct(BigDecimal price, int stockQuantity) {
    Product product = Product.createNew(
        UUID.randomUUID(),
        UUID.randomUUID(),
        "Product " + UUID.randomUUID(),
        "Product description",
        price,
        stockQuantity
    );
    product.activate();
    return product;
  }

  private Order orderForCustomer(UUID customerId, UUID sellerId) {
    return orderForCustomer(customerId, sellerId, sellerId);
  }

  private Order orderForCustomer(UUID customerId, UUID firstSellerId, UUID secondSellerId) {
    UUID orderId = UUID.randomUUID();
    OrderItem firstItem = OrderItem.createNew(
        orderId,
        UUID.randomUUID(),
        firstSellerId,
        "First product",
        BigDecimal.TEN,
        1
    );
    OrderItem secondItem = OrderItem.createNew(
        orderId,
        UUID.randomUUID(),
        secondSellerId,
        "Second product",
        BigDecimal.valueOf(5),
        1
    );
    return Order.createNew(customerId, List.of(firstItem, secondItem));
  }
}
