package com.example.marketplace.user.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import com.example.marketplace.PostgresTestContainerConfig;
import com.example.marketplace.user.domain.User;
import com.example.marketplace.user.domain.UserRepository;
import com.example.marketplace.user.domain.UserRole;
import com.example.marketplace.user.domain.UserStatus;
import reactor.test.StepVerifier;

@ActiveProfiles("test")
@SpringBootTest
class UserRepositoryIntegrationTests {

  @DynamicPropertySource
  static void postgresProperties(DynamicPropertyRegistry registry) {
    PostgresTestContainerConfig.configure(registry);
  }

  @Autowired
  private UserRepository userRepository;

  @Test
  void saveUser() {
    User user = newUser("save-" + UUID.randomUUID() + "@example.com");

    StepVerifier.create(userRepository.save(user))
        .assertNext(saved -> {
          assertThat(saved.getId()).isEqualTo(user.getId());
          assertThat(saved.getEmail()).isEqualTo(user.getEmail());
          assertThat(saved.getPasswordHash()).isEqualTo(user.getPasswordHash());
          assertThat(saved.getRole()).isEqualTo(UserRole.CUSTOMER);
          assertThat(saved.getStatus()).isEqualTo(UserStatus.ACTIVE);
        })
        .verifyComplete();
  }

  @Test
  void findUserById() {
    User user = newUser("find-id-" + UUID.randomUUID() + "@example.com");

    StepVerifier.create(userRepository.save(user).flatMap(saved -> userRepository.findById(saved.getId())))
        .assertNext(found -> {
          assertThat(found.getId()).isEqualTo(user.getId());
          assertThat(found.getEmail()).isEqualTo(user.getEmail());
        })
        .verifyComplete();
  }

  @Test
  void findUserByEmail() {
    User user = newUser("find-email-" + UUID.randomUUID() + "@example.com");

    StepVerifier.create(userRepository.save(user).flatMap(saved -> userRepository.findByEmail(saved.getEmail())))
        .assertNext(found -> {
          assertThat(found.getId()).isEqualTo(user.getId());
          assertThat(found.getEmail()).isEqualTo(user.getEmail());
        })
        .verifyComplete();
  }

  @Test
  void existsByEmail() {
    User user = newUser("exists-" + UUID.randomUUID() + "@example.com");

    StepVerifier.create(userRepository.save(user).flatMap(saved -> userRepository.existsByEmail(saved.getEmail())))
        .expectNext(true)
        .verifyComplete();
  }

  private User newUser(String email) {
    return User.createNewActiveUser(email, "password-hash", UserRole.CUSTOMER);
  }
}
