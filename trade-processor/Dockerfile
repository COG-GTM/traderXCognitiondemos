# Build stage
FROM eclipse-temurin:21-jdk AS builder

WORKDIR /trade-processor
COPY . .
RUN ./gradlew build -x test

# Runtime stage
FROM eclipse-temurin:21-jre-alpine

WORKDIR /trade-processor
COPY --from=builder /trade-processor/build/libs/*.jar app.jar

EXPOSE 18091

ENTRYPOINT ["java", "-jar", "app.jar"]