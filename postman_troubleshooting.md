# Postman Collection — Troubleshooting & Fix Guide

## Root Causes Found

### 1. 🔴 `JWT_SECRET` is missing in the gateway (CRITICAL — breaks ALL requests)

The gateway's `application.yml` declares:
```yaml
jwt:
  secret: ${JWT_SECRET}   # ← NO default fallback!
```
Every other service has a fallback:
```yaml
jwt:
  secret: ${JWT_SECRET:404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970}
```
Without `JWT_SECRET` set as an env variable, the gateway **cannot validate any JWT** and every protected request returns `401 Unauthorized`.

**Fix:** Add the same default fallback in `api-gateway/src/main/resources/application.yml`.

---

### 2. 🔴 `/api/v1/aggregator/**` route is missing from the gateway (CRITICAL)

The Postman collection sends `GET {{gateway_url}}/api/v1/aggregator/order-details/{{order_id}}` but the gateway has **no route** for `/api/v1/aggregator/**`. That path is served by the `order-service` on port 8082.

**Fix:** Add the aggregator route to the gateway config pointing to `order-service`.

---

### 3. 🟡 `X-Idempotency-Key` header is missing from gateway's CORS `allowedHeaders`

The Postman "Create Order" request sends `Idempotency-Key`, but the gateway only allows `X-Idempotency-Key` in CORS. The header name in the Postman collection uses `Idempotency-Key` (no `X-` prefix), so CORS preflight will block it.

**Fix:** Add `Idempotency-Key` to the allowed headers list in `api-gateway/src/main/resources/application.yml`.

---

### 4. 🟡 Rate limiter causes slow responses (Redis timeout = 429s on cold start)

The gateway's `RequestRateLimiter` filter on user-service and order-service connects to Redis. If Redis is not running locally, every request **blocks for the Redis connection timeout** before responding. This is why Postman feels very slow.

**Fix:** Either run Redis locally, or disable the rate limiter filter for local dev by using a Spring profile.

---

### 5. 🟡 Redis is required (but not started) for caching + rate limiting

All services use Redis cache (`spring.cache.type: redis`). If Redis isn't running:
- Every cached read blocks/fails
- The gateway's rate limiter stalls every request

**Fix:** Start Redis, or switch to `spring.cache.type: simple` for local dev.

---

## Fixes Applied

### Fix 1 — Gateway `application.yml`: Add JWT secret default + aggregator route + Idempotency-Key header

See: [application.yml](file:///D:/Projects/New%20folder/Java21_Springboot_Microservices/Java_21_Microservices_FullStack_Guide/api-gateway/src/main/resources/application.yml)

```diff
 jwt:
-  secret: ${JWT_SECRET}
+  secret: ${JWT_SECRET:404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970}
```

```diff
             allowedHeaders:
               - Authorization
               - Content-Type
               - X-Idempotency-Key
+              - Idempotency-Key
```

```diff
         - id: notification-service
           uri: ${NOTIFICATION_SERVICE_URL:http://localhost:8084}
           predicates:
             - Path=/api/v1/notifications/**
+        - id: aggregator
+          uri: ${ORDER_SERVICE_URL:http://localhost:8082}
+          predicates:
+            - Path=/api/v1/aggregator/**
+          filters:
+            - name: CircuitBreaker
+              args:
+                name: orderServiceBreaker
+                fallbackUri: forward:/fallback/order
```

---

## Local Dev Startup Checklist

Run these in order before hitting Postman:

| Step | Command / Action |
|------|-----------------|
| 1 | Start PostgreSQL on port 5432, DB name `engine` |
| 2 | Start Redis on port 6379 (`redis-server`) |
| 3 | Start Kafka + Zookeeper on port 9092 |
| 4 | Start **user-service** (port 8081) |
| 5 | Start **product-service** (port 8083) |
| 6 | Start **order-service** (port 8082) |
| 7 | Start **notification-service** (port 8084) |
| 8 | Start **api-gateway** (port 8080) |

> [!IMPORTANT]
> All services share the same DB (`engine`) and same JWT secret default.
> Run Flyway migrations automatically on startup (`ddl-auto: validate` requires the schema to already exist).

---

## Postman Correct Call Sequence

```
1. Register User        POST /api/v1/auth/register      (no token needed)
2. Login User           POST /api/v1/auth/login         (saves access_token, user_id)
3. Create Product       POST /api/v1/products           (saves product_id)
4. Create Order         POST /api/v1/orders             (saves order_id)
5. Get Order (Agg)      GET  /api/v1/aggregator/order-details/{{order_id}}
```

> [!TIP]
> Use **"Direct Service Access"** folder in Postman to bypass the gateway and JWT.
> This lets you test individual services even if the gateway is not running.

---

## Quick Verification (curl)

```bash
# 1. Gateway health
curl http://localhost:8080/actuator/health

# 2. All service healths
curl http://localhost:8081/actuator/health   # user-service
curl http://localhost:8082/actuator/health   # order-service
curl http://localhost:8083/actuator/health   # product-service
curl http://localhost:8084/actuator/health   # notification-service

# 3. Test Redis is running
redis-cli ping   # should return: PONG
```
