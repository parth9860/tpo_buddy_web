# ==============================================================================
# Production Dockerfile for TPO Buddy (Spring Boot 3 + Java 21)
# Multi-stage build for minimal container footprint and high security
# ==============================================================================

# Stage 1: Build the Application
FROM maven:3.9.6-eclipse-temurin-21 AS builder
WORKDIR /workspace

# Cache Maven dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code and build war package
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Production Runtime
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# Run as non-root user for security
RUN groupadd -r tpogroup && useradd -r -g tpogroup tpouser

# Copy built war package from builder stage
COPY --from=builder /workspace/target/tpo-buddy.war /app/tpo-buddy.war

# Expose default application port
EXPOSE 8085

# Healthcheck to monitor container uptime
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8085/actuator/health || exit 1

USER tpouser

ENV SERVER_PORT=8085 \
    SPRING_PROFILES_ACTIVE=prod

ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "/app/tpo-buddy.war"]
