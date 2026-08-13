FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

COPY pom.xml .
RUN mvn -B dependency:go-offline

COPY src ./src
RUN mvn -B clean package -DskipTests

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

RUN useradd --create-home --shell /bin/bash appuser && chown appuser:appuser /app
RUN mkdir -p /app/uploads /app/uploads-private && chown -R appuser:appuser /app/uploads /app/uploads-private

COPY --from=build --chown=appuser:appuser /build/target/escobar-club.jar app.jar

USER appuser

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
