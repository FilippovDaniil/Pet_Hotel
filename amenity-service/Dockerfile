# syntax=docker/dockerfile:1
FROM gradle:jdk17-alpine AS builder
WORKDIR /workspace
COPY . .
RUN --mount=type=cache,target=/root/.gradle \
    gradle :amenity-service:build -x test --no-daemon

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=builder /workspace/amenity-service/build/libs/*.jar app.jar
EXPOSE 8084
ENTRYPOINT ["java", "-jar", "app.jar"]
