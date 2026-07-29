# Condensed Architecture & Testing Notes
*(Full-Stack Java 21 / Spring Boot 3 Microservices Project)*

---

## 1. System Architecture

**Stack:** React 19 + Vite client → Spring Cloud Gateway (:8080) → 5 Spring Boot 3.3 microservices (User :8081, Product :8083, Order :8082, Payment :8085, Notification :8084) → PostgreSQL 16, Redis 7.2, Kafka 3.8, Jaeger/OpenTelemetry tracing.

**Request flow (order + payment):**
1. Client → Gateway (Bearer JWT, RS256 verified).
2. Gateway routes to Order Service; Order Service calls Product Service to lock/deduct inventory (Redisson `RLock`).
3. Order written to Postgres + `outbox_events` in one ACID transaction.
4. Virtual-thread scheduler relays outbox rows to Kafka (`order-events`) → Notification Service sends confirmation.
5. Payment: client fetches RSA-2048 public key, encrypts card client-side, posts with an `Idempotency-Key`. Payment Service verifies idempotency, writes SUCCESS + outbox event, relays to `payment-events` → Order Service marks order `CONFIRMED`.

**Key architectural trade-offs:**

| Decision | Chosen | Rejected | Why |
|---|---|---|---|
| Distributed transactions | Choreography Saga + Outbox | 2PC/XA | No global locks/blocking; at-least-once delivery |
| Concurrency | Java 21 Virtual Threads | Reactive (WebFlux/RxJava) | Imperative code, no callback hell, no thread exhaustion |
| Inventory locking | Redisson `RLock` | Postgres `SELECT FOR UPDATE` | Moves contention off primary DB to sub-ms Redis |
| Gateway auth | RS256 asymmetric JWT | Introspection/session DB | Stateless, no per-request IdP round-trip |
| Payment security | Client-side RSA-2048 tokenization | Raw PAN transmission | Keeps backend out of PCI-DSS scope |

**Other notable decisions:** Kubernetes-native DNS replaces Eureka for service discovery; AOP `@Around` aspect gives centralized audit logging; SSE (WebFlux + Reactor Sinks) for real-time notifications; dual-profile auth supports either lightweight local JWT or full Keycloak/OAuth2.

**Late-stage hardening:** Axios interceptors auto-logout on 503/504; Order Service's aggregator degrades gracefully instead of crashing when Product Service errors; Gateway CORS tuned for preflight `OPTIONS`; Redis cache serialization fixed with `@class` metadata to avoid `InvalidTypeIdException` on polymorphic types.

---

## 2. Core Design Patterns (Reference List)

1. **Database-per-Service** — each service owns its schema; cross-service reads go through REST/Kafka, never a shared DB.
2. **Transactional Outbox** — entity write + event row committed in one local transaction, solving the dual-write problem; a relay polls and publishes to Kafka.
3. **Saga Choreography** — services react to each other's events (`ORDER_CREATED` → inventory deduction → `INVENTORY_REJECTED` triggers a compensating cancellation) instead of 2PC.
4. **API Gateway** — single entry point; Spring Cloud Gateway handles routing, JWT auth, CORS, rate limiting, circuit breaking.
5. **BFF / Aggregator** — one service fans out parallel calls (via `CompletableFuture` on virtual threads) and merges results so the client makes one request instead of many.
6. **Resilience4j trio** — Circuit Breaker (opens above a failure-rate threshold), Retry (idempotent GETs, exponential backoff), Bulkhead (caps concurrent calls to protect shared thread pools).
7. **Distributed Locking (Redisson `RLock`)** — mutex per SKU avoids optimistic-lock retry storms under burst traffic.
8. **Idempotent Consumer** — Redis `SETNX` on event ID guards against Kafka's at-least-once redelivery causing duplicate processing.
9. **Correlation ID / MDC propagation** — gateway-issued `X-Correlation-ID` flows through every service's logs for cross-service tracing in ELK/Loki.
10. **IDOR ownership checks** — every resource access verifies the JWT's `userId` claim matches the resource owner (or grants `ADMIN`).
11. **PCI-DSS payment design** — RSA-2048 client-side tokenization, sealed-interface pattern matching over 22 payment instruments, unique `idempotency_key`, immutable payment audit log.

---

## 3. Tech Stack Highlights

- **Java 21:** Virtual Threads (`spring.threads.virtual.enabled=true`) for cheap concurrent I/O; Records for immutable DTOs; pattern-matching `switch` for event routing.
- **Spring Boot 3:** MVC for standard services, WebFlux for the reactive gateway; Spring Data JPA/Hibernate; Spring Kafka; Flyway migrations.
- **Frontend:** React + Vite, route-level code splitting (`React.lazy`/`Suspense`, −84% initial bundle size), shimmer skeleton loaders (avoids layout shift), global error boundary, offline banner via online/offline listeners, Zod + React Hook Form schema validation.
- **Data/Messaging:** HikariCP-tuned Postgres; Redis for rate-limit buckets + idempotency keys (24h TTL); Redisson for locks/object caching; Kafka topics partitioned (3), manual-immediate ack, per-purpose consumer groups.
- **Security:** Keycloak/OAuth2 OIDC, stateless JWT validation via JWKS, `@PreAuthorize` RBAC on backend + `AuthContext` guard on frontend.

---

## 4. DevOps & Deployment

- **Docker:** multi-stage builds (Maven→JRE 21); compose files split infra vs. app services.
- **Kubernetes:** liveness/readiness probes via Actuator; graceful shutdown (30s drain on `SIGTERM`).
- **Release strategies:** Blue-Green (instant full cutover once green passes readiness), Canary (small-percentage rollout monitored via Prometheus before full release), Expand-Contract schema migrations (add nullable column → dual-write → drop old column) to avoid breaking older pods.

---

## 5. Interview Q&A — One-Line Answers

| # | Question | Answer |
|---|---|---|
| 1 | Why database-per-service? | Avoids cross-service coupling/fault propagation; sync via Kafka events. |
| 2 | Why transactional outbox? | Solves dual-write problem — entity + event committed atomically, relay publishes to Kafka. |
| 3 | How are distributed transactions handled? | Saga choreography with compensating transactions on failure, not 2PC. |
| 4 | Circuit Breaker vs Bulkhead? | Breaker trips on failure rate; Bulkhead caps concurrency so one slow service can't starve others. |
| 5 | Race condition on last-item stock? | Redisson distributed lock per SKU instead of optimistic-lock retries. |
| 6 | Preventing duplicate Kafka processing? | Redis `SETNX`-based idempotent consumer guard, 24h TTL. |
| 7 | Why Virtual Threads? | Lightweight JVM threads unmount on blocking I/O, letting one instance handle far more concurrent requests than platform threads. |
| 8 | Tracing a request across services? | Correlation ID propagated via header + SLF4J MDC into every log line. |
| 9 | Frontend performance approach? | Code splitting (−84% bundle), skeleton loaders (no CLS), responsive flexbox + zoom fallback for short viewports. |
| 10 | Zero-downtime deploys? | Readiness probes + graceful shutdown during rolling update; Expand-Contract for schema changes. |

---

## 6. RBAC & Integration Test Coverage (Summary)

**Setup:** Gateway validates RS256 JWTs and injects trusted `X-Auth-User` / `X-Auth-Role` headers downstream. Test actors: Admin, User1, User2 (cross-tenant checks), Anonymous/attacker.

**Security matrix (always enforced):**
- No token → `401`.
- Tampered/expired JWT → `401`.
- Client-forged `X-Auth-Role` header → Gateway strips/overwrites it with the verified claim → `403`.
- Excessive login attempts → `429` (Redis token-bucket limiter).

**Per-module pattern (repeats across User, Product, Order, Payment, Notification services):**
- **Admin:** full read/write — view any record, manage catalog (create/update/delete SKUs), override order status, issue refunds, audit payments/notifications.
- **Regular user:** read/write only their own data (profile, orders, payments); catalog is read-only; any attempt to touch another user's resource or an admin-only route → `403`.
- **Payment-specific:** idempotency key replay returns the original result (`200`, same ID) instead of double-charging; a second charge attempt with the same key never creates a new DB row.
- **Notifications:** SSE stream pushes real-time order-status events to the owning user.

**End-to-end scenarios:**
- **Happy path:** user checkout → inventory lock/decrement → order `PENDING` → payment `SUCCESS` → Kafka relay → order `CONFIRMED` → notification sent → aggregator reflects final state.
- **Admin governance:** admin restocks SKU → user sees updated stock immediately → admin cancels a user's order → cancellation notice dispatched.
- **Privilege-escalation audit:** user blocked from `/api/admin/**`, cross-tenant order reads, cross-tenant refunds, and header-spoofing attempts — all `403`, since the gateway always overwrites client-supplied identity headers with verified JWT claims.

**Automation:** JUnit 5 + REST-assured + Testcontainers (Postgres), tokens acquired once in `@BeforeEach`, tests ordered by module. CI/CD gate requires: all the above status codes hold, idempotent payment replay doesn't duplicate rows, and Saga outbox events produce exactly-once Kafka messages that correctly transition order state.
