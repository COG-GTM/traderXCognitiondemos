# Java 21 base image for H2 database service
ARG VARIANT="21"
FROM mcr.microsoft.com/vscode/devcontainers/java:1-${VARIANT}

WORKDIR /database
COPY . .

EXPOSE 18082
EXPOSE 18083
EXPOSE 18084

RUN ./gradlew build
RUN chmod +x ./run.sh
ENTRYPOINT ./run.sh 