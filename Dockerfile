# 1단계: 빌드
FROM gradle:8.3-jdk17 AS builder
WORKDIR /app

# 의존성 캐싱을 위해 build.gradle만 먼저 복사
COPY build.gradle settings.gradle ./
RUN gradle dependencies --no-daemon

# 나머지 소스 코드 복사 후 빌드
COPY . .
RUN gradle clean bootJar --no-daemon

# 2단계: 실행
FROM eclipse-temurin:17-jdk-alpine
WORKDIR /app
COPY --from=builder /app/build/libs/*.jar app.jar
ENV SPRING_PROFILES_ACTIVE=prod
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]