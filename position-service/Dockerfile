# Build stage
FROM eclipse-temurin:21-jdk AS builder

WORKDIR /position-service
COPY . .
RUN ./gradlew build -x test

# Runtime stage
FROM eclipse-temurin:21-jre-alpine

WORKDIR /position-service
COPY --from=builder /position-service/build/libs/*.jar app.jar

EXPOSE 18090

ENTRYPOINT ["java", "-jar", "app.jar"]