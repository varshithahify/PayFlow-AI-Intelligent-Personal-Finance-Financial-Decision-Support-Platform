# ==========================================
# Stage 1: Build
# ==========================================

FROM eclipse-temurin:21-jdk AS build

WORKDIR /workspace

COPY . .

RUN chmod +x ./mvnw && ./mvnw -B -DskipTests clean package


# ==========================================
# Stage 2: Runtime
# ==========================================

FROM eclipse-temurin:21-jre

# Install curl for container health checks
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl ca-certificates \
    && apt-get clean \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

# Create non-root application user
RUN groupadd --system --gid 10001 app \
    && useradd --system --uid 10001 \
       --gid app --home-dir /app \
       --shell /usr/sbin/nologin app

# Copy executable JAR
COPY --from=build --chown=app:app \
    /workspace/target/payflow-backend-0.0.1-SNAPSHOT.jar \
    /app/app.jar

USER app

EXPOSE 8081

ENTRYPOINT ["java", "-jar", "/app/app.jar"]