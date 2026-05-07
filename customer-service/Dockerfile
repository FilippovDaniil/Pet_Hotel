# syntax=docker/dockerfile:1
# Включает BuildKit-синтаксис (--mount=type=cache). Должна быть первой строкой.

# ── Стадия 1: сборка ─────────────────────────────────────────────────────────
# gradle:jdk17-alpine содержит JDK + Gradle CLI; alpine минимизирует размер образа
FROM gradle:jdk17-alpine AS builder
WORKDIR /workspace
# context: . в docker-compose — весь multi-project; Gradle видит :common и все сервисы
COPY . .
# --mount=type=cache: кэш ~/.gradle между сборками → зависимости не скачиваются повторно
# -x test: тесты не запускаются в Docker (запускаются отдельно в CI)
# --no-daemon: daemon-процесс не нужен в одноразовом контейнере сборки
RUN --mount=type=cache,target=/root/.gradle \
    gradle :customer-service:build -x test --no-daemon

# ── Стадия 2: runtime-образ ──────────────────────────────────────────────────
# eclipse-temurin:17-jre-alpine: только JRE (не JDK) → образ ~100 МБ вместо ~300 МБ
# builder-стадия не попадает в финальный образ — кэш Gradle и исходники не утекают
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
# *.jar: Spring Boot fat-jar с именем customer-service-*.jar (версия в имени)
COPY --from=builder /workspace/customer-service/build/libs/*.jar app.jar
EXPOSE 8081
# exec form (JSON-массив): java = PID 1 → Docker корректно доставляет SIGTERM при остановке
ENTRYPOINT ["java", "-jar", "app.jar"]
