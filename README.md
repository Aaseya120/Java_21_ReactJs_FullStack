# Java 21 · Spring Boot 3.3 · Microservices Full-Stack Project

A production-ready, full-stack microservices project demonstrating a modern React frontend and a Java 21 / Spring Boot 3.3 backend with Spring Cloud, Apache Kafka (KRaft mode), Redis, and PostgreSQL.

---

## 🛠️ Tech Stack

### Frontend
| Category | Technology |
|----------|------------|
| **Core** | React 18 + Vite 5 |
| **Routing** | React Router v6 |
| **State** | TanStack Query v5 |
| **Forms** | React Hook Form |
| **HTTP** | Axios + JWT Interceptors |

### Backend & Infrastructure
| Category | Technology |
|----------|------------|
| **Backend** | Java 21 + Spring Boot 3 |
| **Gateway** | Spring Cloud Gateway |
| **Messaging** | Apache Kafka |
| **Database** | PostgreSQL 16 |
| **Cache** | Redis 7 |

---

## 🏗️ Architecture

```
Client (Browser/Mobile)
        │
        ▼ HTTPS
┌─────────────────────────────────────────────┐
│          API Gateway  :8080                 │
│  [JWT Auth · Rate Limiting · Circuit Break] │
└─────────┬───────────┬────────────┬──────────┘
          │           │            │
          ▼           ▼            ▼
   user-service  order-service  product-service  notification-service
      :8081         :8082          :8083              :8084
          │           │            │                    │
          └─────────────────────────────────────────────┘
                           │
              ┌────────────┼────────────┐
              ▼            ▼            ▼
           PostgreSQL    Redis       Apache Kafka 3.8
            :5432        :6379       :9092 (KRaft)
            :5432        :6379       :9092 (KRaft)
```

---

## 📦 Project Structure

```
microservices-demo/
├── pom.xml                       ← Parent POM (Java 21, Spring Boot 3.3.2)
├── docker-compose.yml            ← Full stack (infra + services)
├── docker-compose-infra.yml      ← Infra only (for local IDE dev)
├── README.md
│
├── api-gateway/                  ← Spring Cloud Gateway (:8080)
├── user-service/                 ← Auth, JWT, User profiles (:8081)
├── order-service/                ← Orders, Kafka producer (:8082)
├── product-service/              ← Product catalog, inventory (:8083)
├── notification-service/         ← Kafka consumer, notifications (:8084)
│
└── k8s/                          ← Kubernetes manifests
    ├── namespace.yml
    ├── configmap.yml
    ├── secrets.yml
    ├── api-gateway.yml
    └── microservices.yml
```

---

## 🔧 Prerequisites

| Tool | Version | Purpose |
|------|---------|---------|
| Java JDK | 21 LTS | Runtime |
| Maven | 3.9+ | Build |
| Docker Desktop | 26+ | Containers |
| kubectl | 1.30+ | K8s CLI (prod) |

---

## 🚀 Quick Start — Local Development

### Option 1: IDE + Infra Docker (Recommended)

```bash
# 1. Clone the repository
git clone <repo-url>
cd microservices-demo

# 2. Start infrastructure only (PostgreSQL, Redis, Kafka KRaft, Kafka UI — NO Zookeeper)
docker compose -f docker-compose-infra.yml up -d

# 3. Build all modules
mvn clean package -DskipTests

# 4. Start services (each in a separate terminal)
cd user-service && mvn spring-boot:run
cd order-service && mvn spring-boot:run
cd product-service && mvn spring-boot:run
cd notification-service && mvn spring-boot:run
cd api-gateway && mvn spring-boot:run
```

### Option 2: Full Docker Compose

```bash
# Build all images and start everything
mvn clean package -DskipTests
docker compose up --build -d

# Check logs
docker compose logs -f api-gateway
docker compose logs -f user-service
```

---

## 🗃️ Database Configuration

All services connect to a single PostgreSQL instance:

| Parameter | Value |
|-----------|-------|
| Host | `localhost:5432` |
| Database | `engine` |
| Username | `postgres` |
| Password | `postgres` |

**Schema migrations are handled by Flyway** — tables are created automatically on startup.

---

## 🔐 Security

- **API Gateway**: Validates JWT Bearer tokens on all non-public routes
- **User Service**: Issues JWTs via BCrypt-authenticated login (strength 12)
- **JWT Secret**: Configured via `JWT_SECRET` environment variable

### Public Endpoints (no JWT required)
```
POST /api/auth/register   ← Register a new user
POST /api/auth/login      ← Login and get JWT
GET  /actuator/health     ← Health check
```

---

## 📋 API Reference

### Authentication

```bash
# Register
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"fullName":"John Doe","email":"john@example.com","password":"Secret123!"}'

# Login
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"john@example.com","password":"Secret123!"}'
# Response: {"success":true,"data":{"accessToken":"eyJ...","tokenType":"Bearer",...}}
```

### Orders (requires JWT)

```bash
TOKEN="eyJ..." # from login response

# Create order
curl -X POST http://localhost:8080/api/orders \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "uuid-of-user",
    "productId": "uuid-of-product",
    "quantity": 2,
    "totalPrice": 59.98
  }'

# Get order
curl http://localhost:8080/api/orders/{id} \
  -H "Authorization: Bearer $TOKEN"

# Get user's orders
curl http://localhost:8080/api/orders/user/{userId} \
  -H "Authorization: Bearer $TOKEN"
```

### Products

```bash
# Get all products (paginated)
curl "http://localhost:8080/api/products?page=0&size=10"

# Get by category
curl http://localhost:8080/api/products/category/Electronics

# Decrement stock (after order)
curl -X PUT "http://localhost:8080/api/products/{id}/stock/decrement?qty=1" \
  -H "Authorization: Bearer $TOKEN"
```

---

## ☕ Java 21 Features Used

| Feature | Location |
|---------|----------|
| **Records** | All DTOs: `OrderRequest`, `UserResponse`, `JwtResponse`, `OrderEvent`, `ProductResponse`, `NotificationMessage` |
| **Pattern matching switch** | `OrderResponse.from()`, `OrderService.validateTransition()`, `ProductResponse.from()`, `NotificationService.dispatch()`, `OrderEventConsumer.processOrderEvent()` |
| **Text blocks** | `NotificationService` email templates, `GatewayExceptionHandler` JSON body |
| **Virtual threads** | `spring.threads.virtual.enabled=true` in all services |
| **Sealed types** | `OrderStatus` lifecycle state machine |
| **Guarded patterns** | `ProductResponse.from()` stock level switch |

---

## 🐳 Docker

Each service has a multi-stage Dockerfile:
1. **Stage 1 (builder)**: `eclipse-temurin:21-jdk-alpine` + Maven build
2. **Stage 2 (runtime)**: `eclipse-temurin:21-jre-alpine` with non-root user

**Kafka**: Uses `apache/kafka:3.8.0` in **KRaft mode** — no Zookeeper required.  
**JVM flags**: `-XX:+UseZGC -XX:+ZGenerational` (low-latency GC for Java 21)

---

## ☸️ Kubernetes Deployment

```bash
# Create namespace
kubectl apply -f k8s/namespace.yml

# Create ConfigMap and Secrets
kubectl apply -f k8s/configmap.yml
kubectl apply -f k8s/secrets.yml

# Deploy infrastructure (PostgreSQL, Redis, Kafka via Helm)
helm repo add bitnami https://charts.bitnami.com/bitnami
helm install postgres bitnami/postgresql -n microservices \
  --set auth.username=postgres \
  --set auth.password=postgres \
  --set auth.database=engine

helm install redis bitnami/redis -n microservices \
  --set auth.enabled=false

helm install kafka bitnami/kafka -n microservices \
  --set kraft.enabled=true \
  --set zookeeper.enabled=false

# Deploy services
kubectl apply -f k8s/service-registry.yml
kubectl apply -f k8s/api-gateway.yml
kubectl apply -f k8s/microservices.yml

# Check status
kubectl get pods -n microservices
kubectl get services -n microservices
```

---

## ☁️ AWS EKS Deployment

```bash
# Create EKS cluster
eksctl create cluster \
  --name microservices-cluster \
  --region us-east-1 \
  --nodegroup-name workers \
  --node-type m5.large \
  --nodes 3

# Build and push to ECR
aws ecr create-repository --repository-name user-service
docker build -t user-service ./user-service
docker tag user-service:latest <account-id>.dkr.ecr.us-east-1.amazonaws.com/user-service:latest
aws ecr get-login-password --region us-east-1 | docker login --username AWS --password-stdin <account-id>.dkr.ecr.us-east-1.amazonaws.com
docker push <account-id>.dkr.ecr.us-east-1.amazonaws.com/user-service:latest

# Use AWS RDS for PostgreSQL, ElastiCache for Redis, MSK for Kafka
# Update k8s/configmap.yml with RDS/ElastiCache/MSK endpoints
kubectl apply -k k8s/
```

---

## 🧪 Testing

```bash
# Unit tests
mvn test

# Integration tests (requires Docker)
mvn verify

# Single service tests
cd user-service && mvn test
```

---

## 📊 Monitoring

- **Eureka Dashboard**: http://localhost:8761
- **Kafka UI**: http://localhost:9093
- **Actuator Health**: http://localhost:8080/actuator/health
- **Metrics**: http://localhost:808x/actuator/metrics (each service)

---

## 🛠️ Configuration Reference

| Variable | Default | Description |
|----------|---------|-------------|
| `DB_URL` | `jdbc:postgresql://localhost:5432/engine` | PostgreSQL JDBC URL |
| `DB_USER` | `postgres` | Database username |
| `DB_PASS` | `postgres` | Database password |
| `REDIS_HOST` | `localhost` | Redis hostname |
| `KAFKA_BROKERS` | `localhost:9092` | Kafka bootstrap servers |
| `JWT_SECRET` | dev default | 64-byte hex JWT signing key |

---

## 📄 License

MIT License — free to use for learning and production.
