# ═══════════════════════════════════════════════════════════════════
# Local Development Dockerfile - собирает JAR внутри контейнера
# ═══════════════════════════════════════════════════════════════════
#
# Этот Dockerfile используется для локальной разработки через docker-compose.
# Он самостоятельно собирает JAR из исходников.
#
# Для CI/CD используется Dockerfile.ci, который копирует готовый JAR.
#
# Использование:
#   docker-compose up --build
#   docker build -t currency-bot .
#
# ═══════════════════════════════════════════════════════════════════

# Build stage
FROM gradle:8.5-jdk21 AS build
WORKDIR /app

# Copy gradle files
COPY build.gradle.kts settings.gradle.kts gradle.properties ./
COPY gradle ./gradle

# Download dependencies
RUN gradle dependencies --no-daemon || true

# Copy source code
COPY src ./src

# Build application
RUN gradle buildFatJar --no-daemon

# Runtime stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Copy built jar
COPY --from=build /app/build/libs/*-all.jar app.jar

# Create logs and data directories
RUN mkdir -p /app/logs && \
    mkdir -p /app/data

# Expose port
EXPOSE 8080

# Set environment variables (will be overridden by docker-compose or docker run)
ENV BOT_TOKEN=""
ENV CHAT_ID=""
ENV SCHEDULER_CRON=""
ENV DATABASE_PATH=""
ENV UNKEY_ROOT_KEY=""
ENV UNKEY_INTERNAL_KEY=""

# Run application
ENTRYPOINT ["java", "-jar", "app.jar"]
