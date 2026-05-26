# reactive-marketplace-service

Reactive Marketplace Service is a compact Spring Boot backend for a small online marketplace. It is intended as a
portfolio project that demonstrates clean Java backend structure, reactive persistence, JWT authentication, role-based
access control, Flyway migrations, and tests.

The project intentionally focuses on core marketplace flows: users, authentication, categories, products, and orders. It
does not try to be a full e-commerce platform.

## Tech Stack

- Java 21
- Gradle
- Spring Boot
- Spring WebFlux
- Spring Security
- JWT authentication
- PostgreSQL
- Spring Data R2DBC
- Flyway
- Spring Boot Actuator
- Jakarta Bean Validation
- JUnit 5
- Mockito
- WebTestClient
- Testcontainers PostgreSQL

## Main Features

- Register and login customers and sellers.
- Store passwords as BCrypt hashes.
- Issue JWT access tokens.
- Authenticate protected APIs with a WebFlux-native JWT filter.
- Reject blocked users even when they have a valid JWT.
- Enforce role-based access control.
- Manage categories as an admin.
- Allow sellers to create and manage their own products.
- Allow admins to activate and deactivate products.
- Show only active products through shared product read endpoints.
- Allow customers to create orders from active products.
- Calculate order totals on the backend.
- Store product price, name, and seller snapshots in order items.
- Decrease stock when an order is created.
- Allow customers to view and cancel their own orders.
- Allow sellers to view orders containing their products.
- Allow admins to view all orders and update order status.

## Roles And Permissions

- `CUSTOMER`
    - Read active products and categories.
    - Create orders.
    - View own orders.
    - Cancel own orders before shipped.

- `SELLER`
    - Read active products and categories.
    - Create and update own products.
    - View own products.
    - View orders containing their products.

- `ADMIN`
    - Manage categories.
    - Activate or deactivate products.
    - View all orders.
    - Update order status.

## Architecture Overview

The codebase uses module-oriented packages. Each business module keeps HTTP DTOs, application services, domain models,
and persistence adapters separated.

Controllers stay thin and delegate business decisions to services and domain models. Persistence details stay in
infrastructure adapters. API responses use DTO records and do not expose persistence entities.

## Package Structure

```text
com.example.marketplace
  auth
    api
    application
    domain
    infrastructure
  category
    api
    application
    domain
    infrastructure
  product
    api
    application
    domain
    infrastructure
  order
    api
    application
    domain
    infrastructure
  user
    api
    application
    domain
    infrastructure
  security
  config
  common
    exception
    mapper
    validation
    web
```

## Database Tables

Flyway migration `V1__create_initial_schema.sql` creates:

- `users`
- `categories`
- `products`
- `orders`
- `order_items`

The application uses R2DBC for runtime database access and Flyway over JDBC for migrations.

## Run PostgreSQL

Start the local database with Docker Compose:

```bash
docker compose up -d postgres
```

Default local credentials from `docker-compose.yml`:

- database: `reactive_marketplace`
- user: `marketplace`
- password: `marketplace`
- port: `5432`

## Run The Application

```bash
./gradlew bootRun
```

On Windows PowerShell:

```powershell
.\gradlew bootRun
```

The service starts on port `8080` by default.

## Run Tests

Tests use Testcontainers PostgreSQL for integration coverage:

```bash
./gradlew test
```

Docker must be available for the Testcontainers-backed tests.

## Environment Variables

The application is configured through safe local defaults and can be overridden with environment variables:

```text
SERVER_PORT=8080

SPRING_R2DBC_URL=r2dbc:postgresql://localhost:5432/reactive_marketplace
SPRING_R2DBC_USERNAME=marketplace
SPRING_R2DBC_PASSWORD=marketplace

SPRING_FLYWAY_URL=jdbc:postgresql://localhost:5432/reactive_marketplace
SPRING_FLYWAY_USER=marketplace
SPRING_FLYWAY_PASSWORD=marketplace

JWT_ISSUER=reactive-marketplace-service
JWT_SECRET=change-me-for-local-development-only
JWT_EXPIRATION=15m
```

Use a strong private `JWT_SECRET` outside local development.
The application fails fast outside the `local` and `test` profiles when the default local JWT secret is used.

## Example Requests

Admin registration is not exposed as a public API. For local testing, create an admin user directly in the database or
through a controlled internal process, then login with that account to obtain `<admin-token>`.

Register a customer:

```bash
curl -X POST http://localhost:8080/api/v1/auth/register/customer \
  -H "Content-Type: application/json" \
  -d '{"email":"customer@example.com","password":"password123"}'
```

Register a seller:

```bash
curl -X POST http://localhost:8080/api/v1/auth/register/seller \
  -H "Content-Type: application/json" \
  -d '{"email":"seller@example.com","password":"password123"}'
```

Login:

```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"customer@example.com","password":"password123"}'
```

Get current user:

```bash
curl http://localhost:8080/api/v1/users/me \
  -H "Authorization: Bearer <access-token>"
```

Create a category as admin:

```bash
curl -X POST http://localhost:8080/api/v1/admin/categories \
  -H "Authorization: Bearer <admin-token>" \
  -H "Content-Type: application/json" \
  -d '{"name":"Electronics","description":"Devices and accessories"}'
```

Create a product as seller:

```bash
curl -X POST http://localhost:8080/api/v1/seller/products \
  -H "Authorization: Bearer <seller-token>" \
  -H "Content-Type: application/json" \
  -d '{
    "categoryId":"<category-id>",
    "name":"Wireless Mouse",
    "description":"Compact wireless mouse",
    "price":29.99,
    "stockQuantity":25
  }'
```

Create an order as customer:

```bash
curl -X POST http://localhost:8080/api/v1/customer/orders \
  -H "Authorization: Bearer <customer-token>" \
  -H "Content-Type: application/json" \
  -d '{
    "items":[
      {"productId":"<active-product-id>","quantity":2}
    ]
  }'
```

## Actuator

Public actuator endpoints:

- `GET /actuator/health`
- `GET /actuator/info`

## Error Responses

API errors use a consistent JSON shape:

```json
{
  "timestamp": "2026-05-25T12:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/api/v1/customer/orders",
  "fieldErrors": [
    {
      "field": "items[0].quantity",
      "message": "must be greater than or equal to 1"
    }
  ]
}
```

## Intentionally Not Included

The project intentionally excludes:

- payments
- cart
- delivery
- reviews
- discounts
- Kafka or RabbitMQ
- frontend
- OAuth2
- refresh tokens
