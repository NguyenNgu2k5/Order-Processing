# Final Assessment Report

## Completed requirements

- Flyway schema for users, products, orders, order items, constraints, and all required indexes.
- JWT login, `USER`/`ADMIN` authorization, authenticated principal, and demo seed data.
- Product creation for ADMIN and product persistence.
- Create order with request validation, duplicate rejection, active/product/stock validation, database price calculation, snapshots, and atomic stock reduction.
- Paginated current-user order list sorted by creation time descending.
- Ownership-safe order detail that returns `404` for missing and foreign orders.
- Mock payment with repeated-payment prevention.
- USER cancellation for `PENDING` orders, exact stock restoration, and repeated-cancellation prevention.
- Atomic mock refund for a paid cancelled order.
- ADMIN status updates with the exact allowed transition graph; ADMIN cancellation also restores stock and refunds a paid order.
- Pessimistic product locking in ascending ID order and order-row locking for state-changing operations.
- Consistent business and validation errors without stack trace exposure.
- OpenAPI/Swagger documentation, Postman collection, run instructions, and automated tests.

## Partially completed requirements

None.

## Unfinished requirements

None. The optional ten-request concurrency bonus test is also included.

## Transaction design

`OrderService.create`, `pay`, `cancelMine`, and `updateStatus` are transaction boundaries. Order creation locks and validates every requested product before modifying managed entities. Stock mutation and the order insert therefore commit together. A `BusinessException` is unchecked, so Spring rolls back the transaction automatically.

Cancellation locks the order first, validates its state, locks all referenced products, restores snapshot quantities, updates order/payment states, and commits once. No external call exists in the mock refund flow.

Read operations use read-only transactions so lazy relations can be mapped to response DTOs without exposing entities.

## Locking design

`ProductRepository.findAllByIdForUpdate` uses JPA `PESSIMISTIC_WRITE` with an ascending `ORDER BY`. Concurrent creators for the same product serialize at the database row. Ascending acquisition order prevents two multi-product orders from taking the same locks in opposite order, which reduces deadlock risk.

Payment, cancellation, and ADMIN status updates additionally lock the order row. This makes check-then-update state validation atomic and prevents two concurrent requests from paying, cancelling, refunding, or restoring stock twice.

Java `synchronized` is intentionally not used because it only coordinates threads inside one JVM and cannot protect multiple application instances.

## Refund behavior

- Cancelling `UNPAID` keeps the payment status `UNPAID`.
- Cancelling `PAID` changes it to `REFUNDED` and records `refundedAt` in the same transaction as stock restoration.
- A cancelled order is terminal. A repeated cancellation fails before product locks or stock restoration.
- ADMIN cancellation follows the same shared restoration/refund method as owner cancellation.

## Test summary

16 automated tests pass:

- 13 order-service integration cases for creation, database pricing, empty/duplicate/missing/inactive/insufficient items, all-item validation, ownership, unpaid cancellation, paid refund, repeated cancellation/payment, and transitions.
- 1 web security test proving USER cannot access the ADMIN status endpoint.
- 1 application context test proving the application configuration and Flyway-managed JPA model start successfully.
- 1 concurrent integration test launching 10 simultaneous one-unit orders against stock 5 and verifying exactly 5 successes, final stock 0, and no overselling.

Command: `mvnw.cmd test`

Result: `Tests run: 16, Failures: 0, Errors: 0, Skipped: 0`.

## Known issues and assumptions

- Payment and refund are intentionally internal mock state changes; no payment provider is integrated.
- Demo data initialization is for local assessment use and is disabled under the test profile.
- Order codes use the required `OFL-yyyyMMdd-*` shape plus an eight-character UUID fragment. Database uniqueness is also enforced.
- The JWT implementation is deliberately small and supports this assessment's HMAC access-token flow only. A production identity platform should own key rotation, refresh tokens, revocation, and account lifecycle.
