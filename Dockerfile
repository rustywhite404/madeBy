# Step 1: Gradle 빌드 스테이지
FROM gradle:8.4-jdk21 AS build
WORKDIR /app

# Gradle Wrapper와 소스 코드 복사
COPY . .

# Gradle 빌드 (Eureka Server만)
RUN ./gradlew :eureka-server:bootJar

# Step 2: 실행 스테이지
FROM openjdk:21-jdk-slim
WORKDIR /app

# 빌드된 JAR 파일 복사
COPY --from=build /app/eureka-server/build/libs/*.jar app.jar

# 실행 명령어 지정
ENTRYPOINT ["java", "-jar", "app.jar"]