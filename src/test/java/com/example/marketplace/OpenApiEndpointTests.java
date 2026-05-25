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
class OpenApiEndpointTests {

  @DynamicPropertySource
  static void postgresProperties(DynamicPropertyRegistry registry) {
    PostgresTestContainerConfig.configure(registry);
  }

  @Autowired
  private WebTestClient webTestClient;

  @Test
  void openApiDocsArePublicAndDescribeJwtSecuredMarketplaceEndpoints() {
    webTestClient.get()
        .uri("/v3/api-docs")
        .exchange()
        .expectStatus().isOk()
        .expectBody()
        .jsonPath("$.openapi").isNotEmpty()
        .jsonPath("$.info.title").isEqualTo("Reactive Marketplace Service API")
        .jsonPath("$.components.securitySchemes.bearerAuth.type").isEqualTo("http")
        .jsonPath("$.components.securitySchemes.bearerAuth.scheme").isEqualTo("bearer")
        .jsonPath("$.components.securitySchemes.bearerAuth.bearerFormat").isEqualTo("JWT")
        .jsonPath("$.components.schemas.ApiErrorResponse").exists()
        .jsonPath("$.paths['/api/v1/auth/login'].post.security").doesNotExist()
        .jsonPath("$.paths['/api/v1/customer/orders'].post.security[0].bearerAuth").exists()
        .jsonPath("$.paths['/api/v1/admin/categories'].post.description").isEqualTo("Requires ADMIN role.")
        .jsonPath("$.paths['/api/v1/seller/products'].post.description")
        .isEqualTo("Requires SELLER role. New products are created as DRAFT.")
        .jsonPath("$.paths['/api/v1/customer/orders'].post.responses['409'].content['application/json'].schema.$ref")
        .isEqualTo("#/components/schemas/ApiErrorResponse");
  }

  @Test
  void swaggerUiIsPublic() {
    webTestClient.get()
        .uri("/swagger-ui.html")
        .exchange()
        .expectStatus().is3xxRedirection();
  }
}
