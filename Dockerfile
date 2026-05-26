# syntax=docker/dockerfile:1.7

FROM eclipse-temurin:25-jdk-alpine AS build

WORKDIR /workspace

COPY gradlew settings.gradle build.gradle ./
COPY gradle ./gradle

RUN chmod +x ./gradlew

COPY src ./src

RUN --mount=type=cache,target=/root/.gradle ./gradlew --no-daemon bootJar \
    && mkdir -p /workspace/app \
    && cp "$(find build/libs -maxdepth 1 -name '*.jar' ! -name '*plain*' | head -n 1)" /workspace/app/application.jar

FROM eclipse-temurin:25-jre-alpine

WORKDIR /app

RUN addgroup -S marketplace && adduser -S marketplace -G marketplace

COPY --from=build /workspace/app/application.jar /app/application.jar

USER marketplace:marketplace

EXPOSE 8080

ENV SERVER_PORT=8080

ENTRYPOINT ["java", "-jar", "/app/application.jar"]
