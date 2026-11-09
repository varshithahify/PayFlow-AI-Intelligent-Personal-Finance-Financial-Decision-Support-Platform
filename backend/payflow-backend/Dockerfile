# ==========================================
# Stage 1: Build
# ==========================================

FROM eclipse-temurin:21-jdk AS build

WORKDIR /workspace

# Copy backend source and Maven configuration
COPY . .

# Build executable Spring Boot JAR
RUN chmod +x ./mvnw && ./mvnw -B -DskipTests clean package


# ==========================================
# Stage 2: Runtime
# ==========================================

FROM eclipse-temurin:21-jre

# Apply Ubuntu APT security update
RUN apt-get update \
    && apt-get install -y --only-upgrade apt \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

# Create non-root application user
RUN groupadd --system --gid 10001 app \
    && useradd --system --uid 10001 \
       --gid app --home-dir /app \
       --shell /usr/sbin/nologin app

# Copy executable JAR from build stage
COPY --from=build --chown=app:app \
    /workspace/target/payflow-backend-0.0.1-SNAPSHOT.jar \
    /app/app.jar

# Run application as non-root user
USER app

# Spring Boot application port
EXPOSE 8081

# Start Spring Boot
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
