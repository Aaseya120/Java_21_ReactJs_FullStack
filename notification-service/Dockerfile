# ── Stage 1: Build ──────────────────────────────────────────────
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /build
RUN apk add --no-cache maven
COPY . .
RUN mvn package -pl notification-service -am -DskipTests -q

# ── Stage 2: Runtime ────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine AS runtime
WORKDIR /app
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser
COPY --from=builder /build/notification-service/target/*.jar app.jar
EXPOSE 8084
ENTRYPOINT ["java", "-XX:+UseZGC", "-XX:+ZGenerational", \
  "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
