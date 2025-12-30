# Multi-stage build
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app

# Copy Maven files
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
COPY services/analytics-service/pom.xml services/analytics-service/
COPY libraries/common-models/pom.xml libraries/common-models/

# Download dependencies
RUN ./mvnw dependency:go-offline

# Copy source code
COPY libraries/common-models/src libraries/common-models/src
COPY services/analytics-service/src services/analytics-service/src

# Build
RUN ./mvnw clean package -pl services/analytics-service -am -DskipTests

# Runtime stage
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Create non-root user
RUN addgroup -g 1001 finstream && \
    adduser -u 1001 -G finstream -s /bin/sh -D finstream

# Copy JAR
COPY --from=builder /app/services/analytics-service/target/*.jar app.jar

RUN chown -R finstream:finstream /app

USER finstream

EXPOSE 8082

HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8082/actuator/health || exit 1

ENTRYPOINT ["java", \
    "-XX:+UseZGC", \
    "-XX:+UseStringDeduplication", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-jar", \
    "app.jar"]