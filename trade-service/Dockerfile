# Build stage
FROM eclipse-temurin:21-jdk AS builder

WORKDIR /trade-service
COPY . .
RUN ./gradlew build -x test

# Runtime stage
FROM eclipse-temurin:21-jre-alpine

WORKDIR /trade-service
COPY --from=builder /trade-service/build/libs/*.jar app.jar

EXPOSE 18092

ENTRYPOINT ["java", "-jar", "app.jar"]