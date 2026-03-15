# === Stage 1: Build ===
FROM maven:3.9-eclipse-temurin-21-alpine AS builder

WORKDIR /app

# Copy pom first to leverage Docker layer caching for dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source and build
COPY src ./src
RUN mvn clean package -DskipTests -B

# === Stage 2: Runtime ===
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Add non-root user
RUN addgroup -S estate && adduser -S estate -G estate

COPY --from=builder /app/target/*.jar app.jar

RUN chown estate:estate app.jar
USER estate

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
