package com.example.marketplace.config;

import com.example.marketplace.common.web.ApiErrorResponse;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

  public static final String BEARER_AUTH_SCHEME = "bearerAuth";

  @Bean
  public OpenAPI marketplaceOpenApi() {
    return new OpenAPI()
        .info(new Info()
            .title("Reactive Marketplace Service API")
            .version("v1")
            .description("Reactive backend API for marketplace users, categories, products, and orders."))
        .components(new Components()
            .addSecuritySchemes(BEARER_AUTH_SCHEME, new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
            ));
  }

  @Bean
  public OpenApiCustomizer errorResponseCustomizer() {
    return openApi -> {
      openApi.getComponents()
          .addSchemas("ApiErrorResponse", apiErrorResponseSchema())
          .addSchemas("FieldErrorResponse", fieldErrorResponseSchema());

      openApi.getPaths().values().forEach(pathItem ->
          pathItem.readOperations().forEach(operation -> {
            operation.getResponses().addApiResponse("400", errorResponse("Bad Request"));
            operation.getResponses().addApiResponse("401", errorResponse("Unauthorized"));
            operation.getResponses().addApiResponse("403", errorResponse("Forbidden"));
            operation.getResponses().addApiResponse("404", errorResponse("Not Found"));
            operation.getResponses().addApiResponse("409", errorResponse("Conflict"));
          })
      );
    };
  }

  private ApiResponse errorResponse(String description) {
    return new ApiResponse()
        .description(description)
        .content(new Content().addMediaType(
            org.springframework.http.MediaType.APPLICATION_JSON_VALUE,
            new MediaType().schema(new Schema<ApiErrorResponse>().$ref("#/components/schemas/ApiErrorResponse"))
        ));
  }

  private Schema<?> apiErrorResponseSchema() {
    return new ObjectSchema()
        .description("Standard API error response.")
        .addProperty("timestamp", new StringSchema()
            .format("date-time")
            .description("UTC timestamp when the error response was generated."))
        .addProperty("status", new IntegerSchema()
            .description("HTTP status code.")
            .example(400))
        .addProperty("error", new StringSchema()
            .description("HTTP reason phrase.")
            .example("Bad Request"))
        .addProperty("message", new StringSchema()
            .description("Human-readable error message.")
            .example("Validation failed"))
        .addProperty("path", new StringSchema()
            .description("Request path that failed.")
            .example("/api/v1/customer/orders"))
        .addProperty("fieldErrors", new ArraySchema()
            .items(new Schema<>().$ref("#/components/schemas/FieldErrorResponse"))
            .description("Validation field errors. Empty for non-validation failures."));
  }

  private Schema<?> fieldErrorResponseSchema() {
    return new ObjectSchema()
        .description("Validation error for a single request field.")
        .addProperty("field", new StringSchema()
            .description("Field path from the request body.")
            .example("items[0].quantity"))
        .addProperty("message", new StringSchema()
            .description("Validation failure message.")
            .example("must be greater than or equal to 1"));
  }
}
