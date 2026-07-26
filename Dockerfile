# ---- build stage: Gradle bootJar 생성 ----
FROM gradle:8.14-jdk21 AS build
WORKDIR /app

# 빌드 스크립트 먼저 복사 (의존성 레이어 캐시)
COPY build.gradle settings.gradle ./
COPY gradle ./gradle

# 소스 복사 후 실행가능 bootJar 생성
COPY src ./src
RUN gradle bootJar --no-daemon

# ---- runtime stage: JRE만으로 실행 ----
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
COPY --from=build /app/build/libs/ems-server-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
# JVM 힙 상한은 docker-compose의 JAVA_TOOL_OPTIONS로 주입
ENTRYPOINT ["java", "-jar", "app.jar"]
