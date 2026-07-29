# Microservices Architecture — Simple Guide

A React frontend talks to a Java/Spring Boot backend made of 5 small services, connected through an API Gateway, a database, a cache, and a message queue.

## The Services

| Service | Job |
|---|---|
| Gateway (:8080) | Front door — checks login token, blocks too-many-requests, routes traffic |
| User (:8081) | Login, profile, roles |
| Product (:8083) | Catalog, stock levels |
| Order (:8082) | Places orders, combines data from other services |
| Payment (:8085) | Card charges, refunds |
| Notification (:8084) | Sends emails/SMS/live updates |

**Shared tools:** PostgreSQL (database), Redis (fast cache + locks), Kafka (message queue between services), Jaeger (tracing requests across services).

## How a Purchase Works

1. User clicks "buy" → request goes through the Gateway (which checks the login token).
2. Order Service asks Product Service to reserve stock (using a quick lock in Redis so two people can't buy the last item at once).
3. Order is saved as "PENDING" in the database.
4. In the background, Order Service tells Kafka "an order was created" → Notification Service sees this and sends a confirmation email.
5. User pays: gets an encryption key, encrypts the card in the browser, sends it with a unique "idempotency key" (so re-sending the same payment never charges twice).
6. Payment Service saves the payment as "SUCCESS" → tells Kafka → Order flips to "CONFIRMED".

## Why Things Were Built This Way

| We chose... | Instead of... | Because... |
|---|---|---|
| Each service having its own database | One shared database | So one service's changes can't break another |
| Saga (services reacting to events) | 2-Phase Commit locking | Faster, no service has to freeze and wait for others |
| A Redis lock for stock | Database row locking | Redis is much faster under heavy traffic |
| Java Virtual Threads | Reactive programming | Simpler code, still handles thousands of requests |
| Encrypting card numbers in the browser | Sending raw card numbers to the server | Backend never touches sensitive card data |

## Key Patterns, Explained Simply

- **Outbox Pattern** — When saving an order AND telling Kafka about it, do both in one database transaction. This way, a crash can't leave things half-done.
- **Saga** — Services don't lock each other. They just react: "order created" → "stock reserved" → if something fails, undo the earlier steps.
- **Idempotent Consumer** — If Kafka accidentally delivers the same message twice, the service remembers it already handled that message ID and skips it.
- **Circuit Breaker / Bulkhead** — If a service starts failing a lot, stop calling it for a while (breaker). Also cap how many requests can hit one service at once, so it can't hog all the resources (bulkhead).
- **Correlation ID** — Every request gets a tracking ID that's copied into every service's logs, so you can follow one user's request across all 5 services.
- **Ownership Check** — Before showing someone's data, always confirm the logged-in user actually owns it (or is an admin).

## Frontend Notes

- Built with React + Vite.
- Pages load only when needed (cuts initial load size by ~84%).
- Shows skeleton loading placeholders instead of a spinner, so the page doesn't "jump."
- Shows a banner if the internet connection drops.
- Form validation uses Zod (checks fields like email/password before submitting).

## Deployment

- Runs in Docker containers, managed by Kubernetes.
- Kubernetes checks each service is "alive" and "ready" before sending it traffic.
- When updating, old pods finish their current work (up to 30 seconds) before shutting down — no dropped requests.
- **Blue-Green:** run old and new version side by side, then switch all traffic instantly.
- **Canary:** send a small % of traffic to the new version first, watch for errors, then roll out fully.
- **Database changes:** never delete/rename a column right away — add the new one first, write to both for a while, then remove the old one later.

## Common Interview Questions — Quick Answers

1. **Why separate databases per service?** So services don't get tangled together; each one owns its own data.
2. **What's the Outbox Pattern for?** Prevents "saved to database but Kafka message never sent" bugs.
3. **How do you keep data consistent across services?** Saga pattern — events trigger the next step, and failures trigger an "undo."
4. **Circuit Breaker vs Bulkhead?** Breaker = stop calling a failing service. Bulkhead = limit how much of one service you can use at once.
5. **How do you stop double-selling the last item in stock?** A Redis lock, not database locking — much faster under load.
6. **How do you avoid processing the same Kafka message twice?** Save a "already processed" flag in Redis and check it first.
7. **Why Virtual Threads?** Old threads are expensive (~1MB each); Virtual Threads are cheap, so one server can handle way more waiting requests.
8. **How do you trace one request across 5 services?** A shared tracking ID is attached to every log line.
9. **How was the frontend made fast?** Lazy-loading pages and skeleton loaders.
10. **How do you deploy without downtime?** Readiness checks + graceful shutdown + careful database migrations.
