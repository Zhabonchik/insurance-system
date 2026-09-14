# Build
FROM maven:3.9.6-eclipse-temurin-21-alpine AS builder
WORKDIR /app

# Copy pom.xml
COPY pom.xml .
COPY insurance-service/pom.xml insurance-service/
COPY registry-emulator/pom.xml registry-emulator/

# Copy the source code of the target service
COPY insurance-service/src insurance-service/src

# Builoding insurance-service module
RUN mvn clean package -pl insurance-service -am -DskipTests

# Run
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

COPY --from=builder /app/insurance-service/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]