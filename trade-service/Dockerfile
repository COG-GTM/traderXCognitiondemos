# Java 21 base image for trade-service (Spring Boot 3.5)
ARG VARIANT="21"
FROM mcr.microsoft.com/vscode/devcontainers/java:1-${VARIANT}

WORKDIR /trade-service
COPY . .

EXPOSE 18092

RUN ./gradlew build
ENTRYPOINT ./gradlew bootRun