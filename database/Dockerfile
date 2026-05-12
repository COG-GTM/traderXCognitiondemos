# Build stage
FROM eclipse-temurin:21-jdk AS builder

WORKDIR /database
COPY . .
RUN ./gradlew build && chmod +x ./run.sh

# Runtime stage
FROM eclipse-temurin:21-jre-alpine

RUN apk add --no-cache bash

WORKDIR /database
COPY --from=builder /database/build/libs/database.jar build/libs/database.jar
COPY --from=builder /database/run.sh .
COPY --from=builder /database/initialSchema.sql .

EXPOSE 18082
EXPOSE 18083
EXPOSE 18084

ENTRYPOINT ["./run.sh"]