package com.example.marketplace.auth.application;

import com.example.marketplace.auth.api.LoginRequest;
import com.example.marketplace.auth.api.RegisterRequest;
import com.example.marketplace.common.exception.EmailAlreadyExistsException;
import com.example.marketplace.common.exception.InvalidCredentialsException;
import com.example.marketplace.common.exception.UserBlockedException;
import com.example.marketplace.config.JwtProperties;
import com.example.marketplace.security.JwtTokenProvider;
import com.example.marketplace.user.domain.User;
import com.example.marketplace.user.domain.UserRepository;
import com.example.marketplace.user.domain.UserRole;
import com.example.marketplace.user.domain.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTests {

  private static final String JWT_SECRET = "test-secret-for-jwt-token-provider-12345";

  @Mock
  private UserRepository userRepository;

  private PasswordEncoder passwordEncoder;
  private AuthService authService;

  @BeforeEach
  void setUp() {
    passwordEncoder = new BCryptPasswordEncoder();
    JwtTokenProvider jwtTokenProvider = new JwtTokenProvider(
        new JwtProperties("test-issuer", JWT_SECRET, Duration.ofMinutes(15))
    );
    authService = new AuthService(userRepository, passwordEncoder, jwtTokenProvider);
  }

  @Test
  void registerCustomerSuccessfully() {
    RegisterRequest request = new RegisterRequest(" Customer@Example.com ", "password123");
    when(userRepository.existsByEmail("customer@example.com")).thenReturn(Mono.just(false));
    when(userRepository.save(any(User.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

    StepVerifier.create(authService.registerCustomer(request))
        .assertNext(response -> {
          assertThat(response.accessToken()).isNotBlank();
          assertThat(response.tokenType()).isEqualTo("Bearer");
          assertThat(response.expiresIn()).isEqualTo(900);
          assertThat(response.user().email()).isEqualTo("customer@example.com");
          assertThat(response.user().role()).isEqualTo(UserRole.CUSTOMER);
        })
        .verifyComplete();

    ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
    verify(userRepository).save(userCaptor.capture());
    assertThat(userCaptor.getValue().getPasswordHash()).isNotEqualTo("password123");
    assertThat(passwordEncoder.matches("password123", userCaptor.getValue().getPasswordHash())).isTrue();
  }

  @Test
  void registerSellerSuccessfully() {
    RegisterRequest request = new RegisterRequest("seller@example.com", "password123");
    when(userRepository.existsByEmail("seller@example.com")).thenReturn(Mono.just(false));
    when(userRepository.save(any(User.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

    StepVerifier.create(authService.registerSeller(request))
        .assertNext(response -> {
          assertThat(response.accessToken()).isNotBlank();
          assertThat(response.user().email()).isEqualTo("seller@example.com");
          assertThat(response.user().role()).isEqualTo(UserRole.SELLER);
        })
        .verifyComplete();
  }

  @Test
  void registrationFailsWhenEmailAlreadyExists() {
    RegisterRequest request = new RegisterRequest("existing@example.com", "password123");
    when(userRepository.existsByEmail("existing@example.com")).thenReturn(Mono.just(true));

    StepVerifier.create(authService.registerCustomer(request))
        .expectError(EmailAlreadyExistsException.class)
        .verify();
  }

  @Test
  void registrationMapsDuplicateEmailConstraintToConflictException() {
    RegisterRequest request = new RegisterRequest("race@example.com", "password123");
    when(userRepository.existsByEmail("race@example.com")).thenReturn(Mono.just(false));
    when(userRepository.save(any(User.class))).thenReturn(Mono.error(new DuplicateKeyException("uk_users_email")));

    StepVerifier.create(authService.registerCustomer(request))
        .expectErrorSatisfies(error -> {
          assertThat(error).isInstanceOf(EmailAlreadyExistsException.class);
          assertThat(error).hasMessage("Email already exists: race@example.com");
        })
        .verify();
  }

  @Test
  void loginSucceedsWithCorrectPassword() {
    User user = User.createNewActiveUser(
        "customer@example.com",
        passwordEncoder.encode("password123"),
        UserRole.CUSTOMER
    );
    when(userRepository.findByEmail("customer@example.com")).thenReturn(Mono.just(user));

    StepVerifier.create(authService.login(new LoginRequest("customer@example.com", "password123")))
        .assertNext(response -> {
          assertThat(response.accessToken()).isNotBlank();
          assertThat(response.user().id()).isEqualTo(user.getId());
          assertThat(response.user().email()).isEqualTo(user.getEmail());
        })
        .verifyComplete();
  }

  @Test
  void loginFailsWithWrongPassword() {
    User user = User.createNewActiveUser(
        "customer@example.com",
        passwordEncoder.encode("password123"),
        UserRole.CUSTOMER
    );
    when(userRepository.findByEmail("customer@example.com")).thenReturn(Mono.just(user));

    StepVerifier.create(authService.login(new LoginRequest("customer@example.com", "wrong-password")))
        .expectError(InvalidCredentialsException.class)
        .verify();
  }

  @Test
  void loginFailsForBlockedUser() {
    User activeUser = User.createNewActiveUser(
        "blocked@example.com",
        passwordEncoder.encode("password123"),
        UserRole.CUSTOMER
    );
    User user = User.restore(
        activeUser.getId(),
        activeUser.getEmail(),
        activeUser.getPasswordHash(),
        activeUser.getRole(),
        UserStatus.BLOCKED,
        activeUser.getCreatedAt(),
        activeUser.getUpdatedAt()
    );
    when(userRepository.findByEmail("blocked@example.com")).thenReturn(Mono.just(user));

    StepVerifier.create(authService.login(new LoginRequest("blocked@example.com", "password123")))
        .expectError(UserBlockedException.class)
        .verify();
  }
}
