# Java 21 base image for trade-processor (Spring Boot 3.5)
ARG VARIANT="21"
FROM mcr.microsoft.com/vscode/devcontainers/java:1-${VARIANT}

WORKDIR /trade-processor
COPY . .

EXPOSE 18091

RUN ./gradlew build
ENTRYPOINT ./gradlew bootRun