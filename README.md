# Order Processing and Refund API

Spring Boot backend for transactional order creation, stock locking, payment, cancellation, refund, and administrative status management.

## Technology

- Java 21 and Spring Boot 4.1
- Spring MVC, Security, Data JPA, Validation
- PostgreSQL 17, Flyway, Maven
- JWT authentication (HMAC-SHA256)
- springdoc OpenAPI and Swagger UI
- H2 in PostgreSQL compatibility mode for automated tests

## Run locally

Requirements: Java 21, Docker, and Docker Compose.

```bash
docker compose up -d
./mvnw spring-boot:run
```

On Windows, use `mvnw.cmd spring-boot:run`.

The default database configuration matches `docker-compose.yml`. Copy `.env.example` and override environment variables when needed. Set a strong `JWT_SECRET` outside local development.

## Demo accounts

The application creates these accounts and sample products only when the respective tables are empty:

| Role | Email | Password |
|---|---|---|
| USER | `user@example.com` | `password` |
| USER | `user2@example.com` | `password` |
| ADMIN | `admin@example.com` | `password` |

Login:

```http
POST /api/v1/auth/login
Content-Type: application/json

{"email":"user@example.com","password":"password"}
```

Use the returned token as `Authorization: Bearer <accessToken>`.

## Required APIs

| Method | Path | Access |
|---|---|---|
| POST | `/api/v1/auth/login` | Public |
| POST | `/api/v1/products` | ADMIN |
| POST | `/api/v1/orders` | USER/ADMIN |
| GET | `/api/v1/orders?page=0&size=10` | Owner |
| GET | `/api/v1/orders/{id}` | Owner |
| PUT | `/api/v1/orders/{id}/pay` | Owner |
| PUT | `/api/v1/orders/{id}/cancel` | Owner |
| PUT | `/api/v1/admin/orders/{id}/status` | ADMIN |

## Package structure

- `controller`: REST endpoints only.
- `service`: transaction boundaries and business logic.
- `repository`: Spring Data JPA access and database locking queries.
- `entity`: JPA entities and domain status enums.
- `dto.request`: one request DTO per file.
- `dto.response`: one response DTO per file.
- `dto.CurrentUser`: authenticated principal used internally by Spring Security.
- `config`: JWT filter, Spring Security, OpenAPI, and local demo data.
- `common.exception` / `common.response`: consistent API error handling.

Swagger UI: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)

OpenAPI JSON: [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)

Import `postman/Order-Processing.postman_collection.json` into Postman for ready-to-run requests. The login requests automatically store USER and ADMIN tokens.

## Tests

```bash
./mvnw test
```

The test profile runs against an in-memory database and applies the same Flyway migration. The suite covers successful creation, database prices, validation, ownership, rollback behavior, cancellation, refund, repeated actions, status transitions, role protection, and the ten-request overselling concurrency scenario.

## Important design points

- `OrderService` owns transaction boundaries. Any runtime business error rolls the complete unit of work back.
- Products are sorted by ID and loaded with `PESSIMISTIC_WRITE` before stock changes, preventing overselling and reducing deadlock risk.
- All products are validated before any stock is reduced.
- Order rows are also locked for payment, cancellation, and admin status updates to prevent repeated concurrent actions.
- Ownership is part of the database query. Missing and foreign orders both become `ORDER_NOT_FOUND`.
- `OrderItem` stores product name and price snapshots so historical orders do not change with the product catalog.
- Cancellation restores stock only after validating the current status. A paid cancellation atomically sets `REFUNDED` and `refundedAt`.

See `FINAL_REPORT.md` for the full assessment checklist and implementation notes.
