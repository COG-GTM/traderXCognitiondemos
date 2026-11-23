# Pinned to an exact fixed Temurin build (Java4Shell, CVE-2026-114938: 21 below 21.0.13.1 is affected).
# Do not switch back to a floating tag such as `21` or `21-jdk`; CI checks the image's JDK version.
FROM eclipse-temurin:21.0.13.1_2-jdk-jammy@sha256:4b3d0998ee65462bd86ff060c87119b3575294cb3d0927372afa6f3708b0c8db

WORKDIR /trade-processor
COPY . .

# [Optional] Uncomment this section to install additional OS packages.
# RUN apt-get update && export DEBIAN_FRONTEND=noninteractive \
#     && apt-get -y install --no-install-recommends <your-package-list-here>

# [Optional] Uncomment this line to install global node packages.
# RUN su vscode -c "source /usr/local/share/nvm/nvm.sh && npm install -g <your-package-here>" 2>&1

EXPOSE 18091

RUN ./gradlew build
ENTRYPOINT ./gradlew bootRun