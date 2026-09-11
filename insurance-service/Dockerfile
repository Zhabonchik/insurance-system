# Этап 1: Сборка
FROM maven:3.9.6-eclipse-temurin-21-alpine AS builder
WORKDIR /app

# Копируем родительский pom.xml и pom.xml всех модулей
COPY pom.xml .
COPY insurance-service/pom.xml insurance-service/
COPY registry-emulator/pom.xml registry-emulator/

# Копируем исходный код целевого сервиса
COPY insurance-service/src insurance-service/src

# Собираем только модуль insurance-service
RUN mvn clean package -pl insurance-service -am -DskipTests

# Этап 2: Запуск
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

COPY --from=builder /app/insurance-service/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]