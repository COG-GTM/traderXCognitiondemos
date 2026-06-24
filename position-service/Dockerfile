# Java 21 base image for position-service (Spring Boot 3.5)
ARG VARIANT="21"
FROM mcr.microsoft.com/vscode/devcontainers/java:1-${VARIANT}

WORKDIR /position-service
COPY . .

EXPOSE 18090

RUN ./gradlew build
ENTRYPOINT ./gradlew bootRun