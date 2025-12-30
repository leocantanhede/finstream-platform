FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
COPY services/notification-service/pom.xml services/notification-service/
COPY libraries/common-models/pom.xml libraries/common-models/

RUN ./mvnw dependency:go-offline

COPY libraries/common-models/src libraries/common-models/src
COPY services/notification-service/src services/notification-service/src

RUN ./mvnw clean package -pl services/notification-service -am -DskipTests

FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

RUN addgroup -g 1001 finstream && \
    adduser -u 1001 -G finstream -s /bin/sh -D finstream

COPY --from=builder /app/services/notification-service/target/*.jar app.jar

RUN chown -R finstream:finstream /app

USER finstream

EXPOSE 8083

HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8083/actuator/health || exit 1
	
ENTRYPOINT ["java", \
    "-XX:+UseZGC", \
    "-XX:+UseStringDeduplication", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-jar", \
    "app.jar"]