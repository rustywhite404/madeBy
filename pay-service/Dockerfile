# Gradle 빌드를 위한 베이스 이미지
FROM gradle:8.5-jdk21 AS build
WORKDIR /app

# 모든 필요한 파일 복사
COPY settings.gradle build.gradle gradlew /app/
COPY gradle /app/gradle/
COPY shared /app/shared/
COPY pay-service /app/pay-service/
COPY .env /app/.env

# service 빌드
RUN ./gradlew :pay-service:build -x test --no-daemon

# 런타임 이미지를 위한 설정
FROM openjdk:21-jdk-slim
WORKDIR /app

# 빌드 산출물 복사
COPY --from=build /app/pay-service/build/libs/*.jar app.jar

# 애플리케이션 실행
ENTRYPOINT ["java", "-jar", "app.jar"]