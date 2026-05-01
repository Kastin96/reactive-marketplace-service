package com.example.marketplace;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.containers.PostgreSQLContainer;

final class PostgresTestContainerConfig {

  private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
      .withDatabaseName("marketplace_test")
      .withUsername("test")
      .withPassword("test");

  private PostgresTestContainerConfig() {
  }

  static void configure(DynamicPropertyRegistry registry) {
    POSTGRES.start();

    registry.add("spring.r2dbc.url", PostgresTestContainerConfig::r2dbcUrl);
    registry.add("spring.r2dbc.username", POSTGRES::getUsername);
    registry.add("spring.r2dbc.password", POSTGRES::getPassword);
    registry.add("spring.flyway.url", POSTGRES::getJdbcUrl);
    registry.add("spring.flyway.user", POSTGRES::getUsername);
    registry.add("spring.flyway.password", POSTGRES::getPassword);
  }

  private static String r2dbcUrl() {
    return "r2dbc:postgresql://%s:%d/%s".formatted(
        POSTGRES.getHost(),
        POSTGRES.getMappedPort(PostgreSQLContainer.POSTGRESQL_PORT),
        POSTGRES.getDatabaseName()
    );
  }
}
