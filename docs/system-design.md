# System Design & Architecture

## High-Level Architecture

The system follows a classic microservices architecture centered around an API Gateway, an event bus (Kafka), and a shared data caching layer (Redis). Service discovery is handled natively by Kubernetes.

```mermaid
graph TB
    subgraph CLIENT_LAYER ["1️⃣ CLIENT TIER (React 19 + Vite Dashboard)"]
        UI["🖥️ OrdersPage.jsx & PaymentModal.jsx<br/>(TanStack Query · Optimistic UI · SSE)"]
    end

    subgraph EDGE_GATEWAY ["2️⃣ INGRESS & SECURITY GATEWAY TIER (:8080)"]
        GW["🌐 Spring Cloud Gateway :8080<br/>(🔒 RS256 JWT Auth · ⚡ Resilience4j Circuit Breaker · 🛡️ Redis Rate Limiter)"]
    end

    subgraph DOMAIN_SERVICES ["3️⃣ MICROSERVICES DOMAIN TIER (Java 21 · Spring Boot 3.3.2)"]
        direction TB
        US["👤 User Service :8081<br/>(JWT Provider · RBAC Claims)"]
        PS["📦 Product Service :8083<br/>(SKU Catalog · @Cacheable · Redisson Inventory Lock)"]
        OS["🛒 Order Service :8082<br/>(Saga Outbox · BFF Aggregator · Virtual Threads)"]
        PAYS["💳 Payment Service :8085<br/>(PCI-DSS RSA-2048 Cryptography · Idempotency Guard)"]
        NS["🔔 Notification Service :8084<br/>(Idempotent Kafka Consumer · SMS & Email Alert)"]
    end

    subgraph DATA_EVENT_MESH ["4️⃣ DISTRIBUTED DATA STORAGE, CACHE & EVENT MESH TIER"]
        direction TB
        RD[("⚡ Redis 7.2 Distributed Cache<br/>(RLock Inventory Lock · Rate Limiter Buckets)")]
        PG[("🐘 PostgreSQL 16 ACID Database<br/>(users · orders · payment_transactions · outbox_events)")]
        KF["🌊 Apache Kafka 3.8 Event Mesh<br/>(order-events · payment-events Idempotent Topics)"]
        JG["🔭 Jaeger Tracing :16686<br/>(OpenTelemetry W3C Correlation Trace Spans)"]
    end

    UI ==>|"1. POST /api/v1/orders (Bearer JWT)"| GW
    GW ==>|"2. Route /users (Validate Token)"| US
    GW ==>|"3. Route /products (Lock Inventory)"| PS
    GW ==>|"4. Route /orders (Create Order PENDING)"| OS
    GW ==>|"5. Route /payments (RSA-2048 Charge)"| PAYS

    PS -.->|"3a. Check Cache @Cacheable"| RD
    PS -->|"3b. Acquire RLock lock:product:{sku}"| RD
    PS -->|"3c. Query SKU Catalog"| PG

    OS -->|"4a. ACID Dual-Commit Order + outbox_events"| PG
    OS ==>|"4b. Virtual Thread Scheduler Relay"| KF

    PAYS -->|"5a. Verify Idempotency & Save Payment"| PG
    PAYS ==>|"5b. Outbox Relay to Kafka"| KF

    KF ==>|"6. Consume Event & Dispatch Alert"| NS
    KF ==>|"7. Consume Payment & Confirm Order"| OS

    OS -.-|"Trace Headers"| JG
    PAYS -.-|"Trace Headers"| JG
    NS -.-|"Trace Headers"| JG
```

### 1. Secure Order Creation & Saga Choreography Sequence
```mermaid
sequenceDiagram
    autonumber
    actor User as User
    
    box rgb(30, 58, 138) CLIENT
    participant UI as React UI
    end
    
    box rgb(22, 78, 99) GATEWAY
    participant GW as Gateway :8080
    end
    
    box rgb(49, 46, 129) MICROSERVICES
    participant OS as Order :8082
    participant PS as Product :8083
    participant NS as Notify :8084
    end
    
    box rgb(6, 78, 59) DATA & MESH
    participant RD as Redis 7.2
    participant DB as Postgres 16
    participant KF as Kafka 3.8
    end

    User->>+UI: Submit Checkout (SKU, Qty)
    UI->>+GW: POST /api/v1/orders (Bearer JWT)
    GW->>GW: Verify RS256 Signature & IP Token Bucket
    GW->>+OS: Dispatch Route /api/v1/orders
    OS->>+PS: Query Catalog & Lock Inventory
    PS->>RD: Acquire RLock("lock:product:{sku}")
    RD-->>PS: Lock Acquired (Sub-ms lease)
    PS-->>-OS: Inventory Available & Locked
    OS->>DB: ACID TX: INSERT Order (PENDING) + outbox_events
    DB-->>OS: SQL TX Committed Successfully
    OS-->>-UI: HTTP 201 Created (Order PENDING)
    UI-->>-User: Render Optimistic Order Confirmation Badge
    Note over OS,KF: Loom Virtual Thread Scheduler Polls outbox_events
    OS->>+KF: Produce "order-events" Topic (Idempotent)
    KF->>+NS: Consume "order-events"
    NS->>NS: Send SMS / Email Confirmation Receipt
    NS-->>-KF: Event Acknowledged
```

### 2. PCI-DSS RSA-2048 Secure Payment Tokenization Sequence
```mermaid
sequenceDiagram
    autonumber
    actor User as User
    
    box rgb(30, 58, 138) CLIENT
    participant UI as PaymentModal
    end
    
    box rgb(49, 46, 129) MICROSERVICES
    participant PAY as Payment :8085
    participant OS as Order :8082
    end
    
    box rgb(6, 78, 59) DATA & MESH
    participant DB as Postgres 16
    participant KF as Kafka 3.8
    end

    User->>+UI: Click "Pay Now" & Select 1 of 6 Instruments
    UI->>+PAY: GET /api/v1/payments/security/public-key
    PAY-->>-UI: Return RSA-2048 Merchant Public Key (PEM)
    UI->>UI: Tokenize & Encrypt PAN (ENC:RSA2048_...)
    UI->>+PAY: POST /api/v1/payments (Header: Idempotency-Key)
    PAY->>DB: Verify unique idempotency_key index
    DB-->>PAY: Key Valid (0% Duplicate Charge Risk)
    PAY->>DB: ACID TX: UPDATE Payment SUCCESS + outbox_events
    DB-->>PAY: SQL TX Committed Successfully
    PAY-->>-UI: HTTP 200 OK (Payment Processed)
    UI-->>-User: Render Paid & Confirmed UI Badge
    Note over PAY,KF: Loom Virtual Thread Scheduler Polls outbox_events
    PAY->>+KF: Produce "payment-events" Topic
    KF->>+OS: Consume "payment-events"
    OS->>DB: UPDATE Order Status -> CONFIRMED
    OS-->>-KF: Event Acknowledged
```

## Architectural Trade-off Evaluation Table

| Architectural Decision | Chosen Pattern | Rejected Alternative | Technical Justification & Trade-off |
| :--- | :--- | :--- | :--- |
| **Distributed Transactions** | **Choreography Saga + ACID Outbox** | **2-Phase Commit (2PC) / XA** | Avoids global locking and synchronous blocking across services. Outbox guarantees at-least-once delivery without distributed deadlocks. |
| **Concurrency Engine** | **Java 21 Virtual Threads (Loom)** | **Reactive Programming (WebFlux/RxJava)** | Retains clean imperative code without thread-pool exhaustion or Callback Hell. |
| **Inventory Locking** | **Redisson Distributed Lock (`RLock`)** | **PostgreSQL `SELECT FOR UPDATE`** | Offloads locking contention from the primary ACID database to sub-millisecond Redis RAM. |
| **API Gateway Security** | **RS256 Asymmetric JWT Validation** | **Introspection Endpoint / Session DB** | Zero-latency, stateless token validation at the edge without querying Identity Provider per request. |
| **Payment Security** | **Client-Side RSA-2048 Tokenization** | **Direct Raw PAN Transmission** | Keeps backend services out of PCI-DSS scope by encrypting cards in the browser before network transit. |

## UI Architecture Visualization
The React frontend includes a **Dynamic Architecture Diagram** built directly into the Login Dashboard using native HTML/CSS (no external charting libraries). It dynamically visualizes the connection between the React Client, API Gateway, Microservices, and the Infrastructure layer (Postgres, Redis, Kafka, Keycloak), complete with animated data packet flows.

## Key Architectural Decisions

1. **Service Discovery**: Removed Eureka in favor of Kubernetes-native DNS routing. Services communicate via direct URLs (e.g., `http://order-service:8082`).
2. **Concurrency**: Utilizes **Java 21 Virtual Threads** (`spring.threads.virtual.enabled=true`) for non-blocking parallel execution across all services.
3. **PCI-DSS Compliant Payment Cryptography & Security**:
   - `payment-service` implements **RSA-2048 Asymmetric Public / Private Key Cryptography** (`PaymentCryptographyService`) for client-side card tokenization and SHA256withRSA signature verification.
   - Leverages **Java 21 Sealed Interfaces** (`PaymentInstrument`, `GatewayExecutionResult`) for exhaustive pattern matching switch across 22 global payment methods.
   - Enforces strict **Idempotency Protection** (`idempotency_key`) and the **Transactional Outbox Pattern** (`payment_outbox_events`).
4. **Data Caching & Locking**: 
   - Uses Spring's `@Cacheable` abstraction with Redis for read-heavy operations.
   - Uses **Redisson** (`RLock`, `RBucket`) for distributed inventory management and concurrency control.
5. **Audit Logging**: A shared `common-module` implements an AOP-based `@Around` aspect to intercept and asynchronously log all controller actions to a centralized `audit_logs` table, while `payment-service` maintains immutable financial audit records in `payment_audit_logs`.
6. **Real-time Notifications**: Implemented via Spring WebFlux Server-Sent Events (SSE) and Reactor Sinks.
7. **Authentication**: 
   - Gateway enforces JWT validation.
   - Dual-profile support allows switching between lightweight local JWTs or a full **OAuth2/Keycloak** Resource Server topology.

## Distributed Tracing & Resilience

- **Resilience4j** is configured on the API Gateway to provide Circuit Breaking and Rate Limiting (via Redis) to protect downstream services.

## Final Development Milestones & Polish
During the final phases of project development, several critical enterprise features and stability improvements were added:
1. **Frontend Global Error Handling**: Axios interceptors were implemented to detect service unavailability (503/504) and automatically log users out safely, preventing the app from hanging.
2. **Aggregator Pattern Stabilization**: The `order-service` implements an API Aggregator that merges Order and Product data. This was fortified to gracefully handle errors from the `product-service` without crashing the page, correctly falling back to generic data when necessary.
3. **CORS & Preflight Integrity**: Gateway CORS configurations were strict-tuned to properly forward preflight `OPTIONS` requests and required headers (`Authorization`, `Content-Type`) preventing modern browsers from blocking legitimate cross-origin XHR requests.
4. **Cache Type Serialization**: Redis caching (`@Cacheable`) was upgraded to properly serialize/deserialize Java Object types by writing `@class` property metadata. This ensures complex polymorphic types like `OrderResponse` or `ProductResponse` can be safely fetched from the cache without `InvalidTypeIdException`.
5. **Local Developer Experience (DX)**: A suite of PowerShell orchestration scripts (`start-all.ps1`, `start-down.ps1`, `stop-all.ps1`) was introduced to dramatically speed up the local Windows development lifecycle.
