# Multi-stage build
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app

# Copy Maven wrapper and pom files
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
COPY services/transaction-ingestion/pom.xml services/transaction-ingestion/
COPY libraries/common-models/pom.xml libraries/common-models/

# Download dependencies
RUN ./mvnw dependency:go-offline

# Copy source code
COPY libraries/common-models/src libraries/common-models/src
COPY services/transaction-ingestion/src services/transaction-ingestion/src

# Build application
RUN ./mvnw clean package -pl services/transaction-ingestion -am -DskipTests

# Runtime stage
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Create non-root user
RUN addgroup -g 1001 finstream && \
    adduser -u 1001 -G finstream -s /bin/sh -D finstream

# Copy JAR from builder
COPY --from=builder /app/services/transaction-ingestion/target/*.jar app.jar

# Change ownership
RUN chown -R finstream:finstream /app

USER finstream

EXPOSE 8080

ENTRYPOINT ["java", \
    "-XX:+UseZGC", \
    "-XX:+UseStringDeduplication", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-jar", \
    "app.jar"]