#FROM openjdk:21-jdk-slim AS builder
#
#WORKDIR /app
#
#COPY mvnw .
#COPY .mvn .mvn
#COPY pom.xml .
#
#RUN ./mvnw dependency:go-offline -B
#
#COPY src src
#
#RUN ./mvnw clean package -DskipTests
#
#EXPOSE 8080
#
#CMD ["java", "-jar", "target/studentbatch-0.0.1-SNAPSHOT.jar"]



FROM openjdk:21-jdk-slim

WORKDIR /app

# Install curl for health checks
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

# Copy Maven wrapper and pom.xml
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

# Download dependencies
RUN ./mvnw dependency:go-offline -B

# Copy source code
COPY src src

# Build application
RUN ./mvnw package -DskipTests

# Create directories for input files and logs
RUN mkdir -p /app/input /app/logs

# Run application
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "target/studentbatch-0.0.1-SNAPSHOT.jar"]