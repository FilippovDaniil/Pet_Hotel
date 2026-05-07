# syntax=docker/dockerfile:1
# Включает BuildKit-синтаксис (--mount=type=cache). Должна быть первой строкой.

# ── Стадия 1: сборка ─────────────────────────────────────────────────────────
FROM gradle:jdk17-alpine AS builder
WORKDIR /workspace
COPY . .
RUN --mount=type=cache,target=/root/.gradle \
    gradle :room-service:build -x test --no-daemon

# ── Стадия 2: runtime-образ ──────────────────────────────────────────────────
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=builder /workspace/room-service/build/libs/*.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]
