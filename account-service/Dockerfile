# Build stage
FROM eclipse-temurin:21-jdk AS builder

WORKDIR /account-service
COPY . .
RUN ./gradlew build -x test

# Runtime stage
FROM eclipse-temurin:21-jre-alpine

WORKDIR /account-service
COPY --from=builder /account-service/build/libs/*.jar app.jar

EXPOSE 18088

ENTRYPOINT ["java", "-jar", "app.jar"]