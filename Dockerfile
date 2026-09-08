# Build Stage
FROM eclipse-temurin:21-jdk AS builder
WORKDIR /workspace

# Gradle 캐싱을 위해 wrapper 및 설정 파일 먼저 복사
COPY gradlew settings.gradle build.gradle ./
COPY gradle ./gradle

# 실행 권한 부여 및 의존성 다운로드
RUN chmod +x ./gradlew && ./gradlew dependencies --no-daemon || true

# 소스 코드 복사
COPY src ./src

# WAR 빌드
RUN ./gradlew bootWar --no-daemon -x test

# Runtime Stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# 비루트 사용자 생성 및 설정
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# 빌드 결과물 복사
COPY --from=builder /workspace/build/libs/*.war app.war

EXPOSE 8080

ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.war"]
