package com.example.marketplace.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.mock.env.MockEnvironment;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtSecretValidatorTests {

  @Test
  void rejectsDefaultSecretOutsideLocalAndTestProfiles() {
    JwtSecretValidator validator = validator(
        JwtSecretValidator.LOCAL_DEVELOPMENT_SECRET,
        environment("prod")
    );

    assertThatThrownBy(() -> validator.run(args()))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("Default JWT secret must not be used outside local/test profiles. Set JWT_SECRET to a strong private value.");
  }

  @Test
  void rejectsShortSecretOutsideLocalAndTestProfiles() {
    JwtSecretValidator validator = validator("short-secret", environment("prod"));

    assertThatThrownBy(() -> validator.run(args()))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("JWT secret must be at least 32 characters outside local/test profiles.");
  }

  @Test
  void allowsStrongSecretOutsideLocalAndTestProfiles() {
    JwtSecretValidator validator = validator(
        "strong-private-jwt-secret-value-123456",
        environment("prod")
    );

    assertThatCode(() -> validator.run(args()))
        .doesNotThrowAnyException();
  }

  @Test
  void allowsDefaultSecretInLocalAndTestProfiles() {
    assertThatCode(() -> validator(JwtSecretValidator.LOCAL_DEVELOPMENT_SECRET, environment("local")).run(args()))
        .doesNotThrowAnyException();
    assertThatCode(() -> validator(JwtSecretValidator.LOCAL_DEVELOPMENT_SECRET, environment("test")).run(args()))
        .doesNotThrowAnyException();
  }

  @Test
  void rejectsDefaultSecretWhenNoProfileIsActive() {
    JwtSecretValidator validator = validator(
        JwtSecretValidator.LOCAL_DEVELOPMENT_SECRET,
        environment()
    );

    assertThatThrownBy(() -> validator.run(args()))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("Default JWT secret must not be used");
  }

  private JwtSecretValidator validator(String secret, MockEnvironment environment) {
    JwtProperties properties = new JwtProperties(
        "reactive-marketplace-service",
        secret,
        Duration.ofMinutes(15)
    );
    return new JwtSecretValidator(properties, environment);
  }

  private MockEnvironment environment(String... profiles) {
    MockEnvironment environment = new MockEnvironment();
    environment.setActiveProfiles(profiles);
    return environment;
  }

  private DefaultApplicationArguments args() {
    return new DefaultApplicationArguments();
  }
}
