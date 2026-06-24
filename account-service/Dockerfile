# Java 21 base image for account-service (Spring Boot 3.5)
ARG VARIANT="21"
FROM mcr.microsoft.com/vscode/devcontainers/java:1-${VARIANT}

WORKDIR /account-service
COPY . .

EXPOSE 18088

RUN ./gradlew build
ENTRYPOINT ./gradlew bootRun