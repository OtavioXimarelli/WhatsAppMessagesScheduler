# Stage 1: Build the application
FROM maven:3-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
# Cache dependencies using Docker BuildKit cache mount
RUN --mount=type=cache,target=/root/.m2 mvn dependency:go-offline -B
COPY src ./src
# Build application using the same cache mount to avoid re-downloading
RUN --mount=type=cache,target=/root/.m2 mvn clean package -DskipTests

# Stage 2: Create the runtime image
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/WhatsAppDailyGroupScheduler-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
