package com.example.marketplace.user.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UserTests {

  @Test
  void createNewActiveUserCreatesActiveUserWithGeneratedIdAndTimestamps() {
    User user = User.createNewActiveUser("customer@example.com", "password-hash", UserRole.CUSTOMER);

    assertThat(user.getId()).isNotNull();
    assertThat(user.getEmail()).isEqualTo("customer@example.com");
    assertThat(user.getPasswordHash()).isEqualTo("password-hash");
    assertThat(user.getRole()).isEqualTo(UserRole.CUSTOMER);
    assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
    assertThat(user.getCreatedAt()).isNotNull();
    assertThat(user.getUpdatedAt()).isNotNull();
  }

  @Test
  void blockChangesStatusToBlocked() {
    User user = User.createNewActiveUser("seller@example.com", "password-hash", UserRole.SELLER);

    user.block();

    assertThat(user.getStatus()).isEqualTo(UserStatus.BLOCKED);
    assertThat(user.getUpdatedAt()).isAfterOrEqualTo(user.getCreatedAt());
  }

  @Test
  void activateChangesStatusToActive() {
    User user = User.createNewActiveUser("admin@example.com", "password-hash", UserRole.ADMIN);
    user.block();

    user.activate();

    assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
    assertThat(user.getUpdatedAt()).isAfterOrEqualTo(user.getCreatedAt());
  }
}
