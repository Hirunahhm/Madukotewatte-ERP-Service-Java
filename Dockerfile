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

# The estate operates in Sri Lanka; all business timestamps (attendance, latex
# records, expenses, ...) are constructed as naive LocalDateTime "wall clock"
# values with no timezone info attached, both by the frontend and by backend
# calls to LocalDateTime.now(). A container defaulting to UTC makes the two
# disagree by 5:30 — e.g. LocalDateTime.now() used as an upper bound in a
# "last N days" query would exclude a same-day record whose naive timestamp
# is still "in the future" from UTC's perspective. Pinning the JVM's own
# timezone (via its bundled tz database, no OS tzdata package needed) keeps
# every LocalDateTime.now() call consistent with what the frontend sends.
ENTRYPOINT ["java", "-Duser.timezone=Asia/Colombo", "-jar", "app.jar"]
