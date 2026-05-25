package com.example.marketplace;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SpringBootTest
class SchemaMigrationTests {

  @DynamicPropertySource
  static void postgresProperties(DynamicPropertyRegistry registry) {
    PostgresTestContainerConfig.configure(registry);
  }

  @Autowired
  private DatabaseClient databaseClient;

  @Test
  void flywayCreatesInitialSchemaTables() {
    Set<String> tableNames = databaseClient.sql("""
            SELECT table_name
            FROM information_schema.tables
            WHERE table_schema = 'public'
              AND table_type = 'BASE TABLE'
            """)
        .map((row, metadata) -> row.get("table_name", String.class))
        .all()
        .collectList()
        .map(Set::copyOf)
        .block();

    assertThat(tableNames)
        .contains("users", "categories", "products", "orders", "order_items");
  }
}
