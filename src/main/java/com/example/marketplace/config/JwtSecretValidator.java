package com.example.marketplace.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Set;

@Component
@RequiredArgsConstructor
class JwtSecretValidator implements ApplicationRunner {

  static final String LOCAL_DEVELOPMENT_SECRET = "change-me-for-local-development-only";
  private static final int MINIMUM_SECRET_LENGTH = 32;
  private static final Set<String> INSECURE_SECRET_ALLOWED_PROFILES = Set.of("local", "test");

  private final JwtProperties jwtProperties;
  private final Environment environment;

  @Override
  public void run(ApplicationArguments args) {
    if (insecureSecretAllowed()) {
      return;
    }

    String secret = jwtProperties.secret();
    if (LOCAL_DEVELOPMENT_SECRET.equals(secret)) {
      throw new IllegalStateException(
          "Default JWT secret must not be used outside local/test profiles. Set JWT_SECRET to a strong private value."
      );
    }
    if (secret.length() < MINIMUM_SECRET_LENGTH) {
      throw new IllegalStateException(
          "JWT secret must be at least %d characters outside local/test profiles.".formatted(MINIMUM_SECRET_LENGTH)
      );
    }
  }

  private boolean insecureSecretAllowed() {
    return Arrays.stream(environment.getActiveProfiles())
        .anyMatch(INSECURE_SECRET_ALLOWED_PROFILES::contains);
  }
}
