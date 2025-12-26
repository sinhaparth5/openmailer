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

# Build Tailwind CSS for production
RUN npm run build:css:prod

# Build Spring Boot application (skip tests for faster builds)
RUN ./mvnw clean package -DskipTests

# Stage 2: Runtime image
FROM eclipse-temurin:25-jre

WORKDIR /app

# Copy the built JAR from builder stage
COPY --from=builder /app/target/openmailer-0.0.1-SNAPSHOT.jar app.jar

# Expose the application port
EXPOSE 8080

# Set production profile by default
ENV SPRING_PROFILES_ACTIVE=prod

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
