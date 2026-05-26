# reactive-marketplace-service

[![CI](https://github.com/Kastin96/reactive-marketplace-service/actions/workflows/ci.yml/badge.svg)](https://github.com/Kastin96/reactive-marketplace-service/actions/workflows/ci.yml)

Reactive Marketplace Service is a Spring Boot backend for core marketplace operations: account registration, JWT
authentication, category management, seller product management, product discovery, and order processing.

The service is intentionally scoped to backend workflows that need clear security boundaries, transactional data access,
database migrations, API contracts, and automated verification.

## Capabilities

- Register and authenticate customers and sellers.
- Issue JWT access tokens and authenticate WebFlux requests with a stateless security filter.
- Reject blocked users even when they present a valid token.
- Enforce role-based access for customer, seller, and admin APIs.
- Manage categories through admin endpoints.
- Let sellers create, update, and list their own products.
- Let admins activate and deactivate products.
- Expose paged product and order list endpoints.
- Create customer orders from active products.
- Reserve product stock atomically during order creation.
- Calculate order totals on the backend.
- Store product name, price, and seller snapshots in order items.
- Let customers view and cancel their own orders.
- Let sellers view orders containing their products.
- Let admins list all orders and update order status.

## Technology

- Java 21
- Gradle
- Spring Boot
- Spring WebFlux
- Spring Security
- JWT
- PostgreSQL
- Spring Data R2DBC
- Flyway
- Spring Boot Actuator
- Springdoc OpenAPI
- Jakarta Bean Validation
- JUnit 5
- Mockito
- WebTestClient
- Testcontainers PostgreSQL
- Docker Compose
- GitHub Actions

## Roles

`CUSTOMER`

- Read active products and categories.
- Create orders.
- View own orders.
- Cancel own orders before shipment.

`SELLER`

- Read active products and categories.
- Create and update own products.
- View own products.
- View orders containing their products.

`ADMIN`

- Manage categories.
- Activate and deactivate products.
- View all orders.
- Update order status.

## Architecture

The codebase uses module-oriented packages. Business modules keep HTTP DTOs, application services, domain models, and
persistence adapters separated.

Controllers expose API contracts and delegate business decisions to application services. Domain models own state
transitions and invariants. Infrastructure adapters isolate R2DBC persistence details from the rest of the application.

```text
com.example.marketplace
  auth
    api
    application
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
    pagination
    web
```

## Database

Flyway migration `V1__create_initial_schema.sql` creates:

- `users`
- `categories`
- `products`
- `orders`
- `order_items`

The application uses R2DBC for runtime database access and Flyway over JDBC for schema migrations.

Already-applied Flyway migrations should not be edited for shared or production-like databases. Add a new `V2__...sql`
migration for schema changes. For local Docker volumes, use `docker compose down -v` when you intentionally want a fresh
database.

## API Documentation

OpenAPI is available when the application is running:

- `GET /v3/api-docs`
- `GET /swagger-ui.html`

Protected endpoints use the `bearerAuth` JWT security scheme.

## Pagination

List endpoints use `page` and `size` query parameters:

```text
GET /api/v1/products?page=0&size=20
GET /api/v1/customer/orders?page=0&size=20
```

Paged responses use this shape:

```json
{
  "content": [],
  "page": 0,
  "size": 20,
  "totalElements": 0,
  "totalPages": 0,
  "last": true
}
```

`size` must be between `1` and `100`.

## Run With Docker Compose

Build and start the full local stack:

```bash
docker compose up --build
```

The API is available at `http://localhost:8080`.

Stop the stack:

```bash
docker compose down
```

Reset the local Compose database volume:

```bash
docker compose down -v
```

## Run PostgreSQL Only

Start only the local database:

```bash
docker compose up -d postgres
```

Default local credentials:

- database: `reactive_marketplace`
- user: `marketplace`
- password: `marketplace`
- port: `5432`

## Run Locally

Bash:

```bash
SPRING_PROFILES_ACTIVE=local ./gradlew bootRun
```

Windows PowerShell:

```powershell
$env:SPRING_PROFILES_ACTIVE='local'
.\gradlew bootRun
```

The service starts on port `8080` by default.

Outside `local` and `test` profiles, the application fails fast if the default local JWT secret is used.

## Run Tests

Tests use Testcontainers PostgreSQL for integration coverage:

```bash
./gradlew test
```

Docker must be available for Testcontainers-backed tests.

## Configuration

The application is configured through environment variables:

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

Use a strong private `JWT_SECRET` outside local development. The default local secret is allowed only in `local` and
`test` profiles.

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

## Operations

Public actuator endpoints:

- `GET /actuator/health`
- `GET /actuator/info`

CI runs `./gradlew test` on pushes and pull requests to `main`.

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

## Service Boundaries

The service does not include:

- payments
- cart
- delivery
- reviews
- discounts
- Kafka or RabbitMQ
- frontend
- OAuth2
- refresh tokens
