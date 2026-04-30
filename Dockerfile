# Multi-stage Dockerfile for OpenMailer
# Stage 1: Build the application
FROM eclipse-temurin:25-jdk AS builder

# Install Node.js for Tailwind CSS compilation
RUN apt-get update && apt-get install -y curl && \
    curl -fsSL https://deb.nodesource.com/setup_20.x | bash - && \
    apt-get install -y nodejs && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

WORKDIR /app

# Copy package files and install Node dependencies
COPY package*.json ./
RUN npm install

# Copy Maven wrapper and pom.xml
COPY mvnw* ./
COPY .mvn .mvn
COPY pom.xml ./

# Download Maven dependencies (cache this layer)
RUN ./mvnw dependency:go-offline -B

# Copy source code
COPY src ./src
COPY tailwind.config.js ./

# Build production frontend assets
RUN npm run build:prod

# Build Spring Boot application (skip tests for faster builds)
RUN ./mvnw clean package -DskipTests

# Create a trimmed Java runtime for the application
RUN mkdir -p /app/target/dependency && \
    cd /app/target/dependency && \
    jar -xf ../openmailer-0.0.1-SNAPSHOT.jar && \
    CLASSPATH="$(echo BOOT-INF/lib/*.jar | tr ' ' ':')" && \
    jdeps \
      --ignore-missing-deps \
      --multi-release 25 \
      --recursive \
      --print-module-deps \
      --class-path "${CLASSPATH}" \
      BOOT-INF/classes > /tmp/java-modules.txt && \
    jlink \
      --add-modules "$(cat /tmp/java-modules.txt),jdk.crypto.ec,jdk.unsupported" \
      --strip-debug \
      --no-man-pages \
      --no-header-files \
      --compress=2 \
      --output /opt/java-minimal

# Stage 2: Runtime image
FROM debian:bookworm-slim

WORKDIR /app

RUN apt-get update && \
    apt-get install -y --no-install-recommends bash vim-tiny ca-certificates && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

ENV JAVA_HOME=/opt/java-minimal
ENV PATH="${JAVA_HOME}/bin:${PATH}"

# Copy the built JAR from builder stage
COPY --from=builder /opt/java-minimal /opt/java-minimal
COPY --from=builder /app/target/openmailer-0.0.1-SNAPSHOT.jar app.jar

# Expose the application port
EXPOSE 8080

# Set production profile by default
ENV SPRING_PROFILES_ACTIVE=prod

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
