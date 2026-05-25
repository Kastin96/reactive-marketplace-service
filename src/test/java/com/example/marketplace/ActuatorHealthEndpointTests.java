package com.example.marketplace;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

@ActiveProfiles("test")
@AutoConfigureWebTestClient
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ActuatorHealthEndpointTests {

  @DynamicPropertySource
  static void postgresProperties(DynamicPropertyRegistry registry) {
    PostgresTestContainerConfig.configure(registry);
  }

  @Autowired
  private WebTestClient webTestClient;

  @Test
  void healthEndpointReturnsOk() {
    webTestClient.get()
        .uri("/actuator/health")
        .exchange()
        .expectStatus().isOk();
  }

  @Test
  void responseContainsRequestIdHeader() {
    webTestClient.get()
        .uri("/actuator/health")
        .exchange()
        .expectHeader().exists("X-Request-Id");
  }

  @Test
  void responseUsesProvidedRequestIdHeader() {
    webTestClient.get()
        .uri("/actuator/health")
        .header("X-Request-Id", "test-request-id")
        .exchange()
        .expectHeader().valueEquals("X-Request-Id", "test-request-id");
  }
}
