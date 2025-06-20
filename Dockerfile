# 1단계: 빌드
FROM gradle:8.3-jdk17 AS builder
WORKDIR /app
COPY --chown=gradle:gradle . .
USER gradle
RUN gradle bootJar --no-daemon

# 2단계: 실행
FROM eclipse-temurin:17-jdk-alpine
WORKDIR /app
COPY --from=builder /app/build/libs/*.jar app.jar
ENV SPRING_PROFILES_ACTIVE=prod
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]