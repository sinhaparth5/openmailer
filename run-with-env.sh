#!/bin/bash

# Load environment variables from .env file and run Spring Boot application

# Check if .env file exists
if [ ! -f .env ]; then
    echo "Error: .env file not found!"
    exit 1
fi

echo "Loading environment variables from .env file..."

# Export all variables from .env file
set -a
source .env
set +a

echo "Environment variables loaded successfully!"
echo "Starting OpenMailer application..."
echo ""

# Run the Spring Boot application
./mvnw spring-boot:run
