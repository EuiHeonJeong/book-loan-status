# syntax=docker/dockerfile:1

########################
# 1. Build stage
########################
FROM eclipse-temurin:21-jdk-jammy AS builder
WORKDIR /app

# 의존성 캐싱: 소스 변경과 무관한 wrapper/빌드 파일부터 복사
COPY gradlew ./
COPY gradle ./gradle
COPY build.gradle settings.gradle ./
RUN chmod +x gradlew

COPY src ./src
RUN ./gradlew --no-daemon bootJar -x test

# bootJar와 plain jar가 둘 다 build/libs에 생기므로 실행 가능한(fat) jar만 고른다.
RUN cp $(ls build/libs/*.jar | grep -v -- '-plain\.jar$') /app/app.jar

########################
# 2. Runtime stage
########################
# build.gradle의 com.microsoft.playwright:playwright 버전과 반드시 일치시킬 것.
# 이 이미지는 해당 버전의 Chromium + 실행에 필요한 OS 의존성이 이미 설치돼 있다.
FROM mcr.microsoft.com/playwright/java:v1.49.0-jammy AS runtime
WORKDIR /app

ENV TZ=Asia/Seoul

COPY --from=builder /app/app.jar ./app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
