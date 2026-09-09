# Node 22 LTS (bookworm-slim) build + runtime for the Angular front end.
ARG NODE_VERSION=22-bookworm-slim

FROM node:${NODE_VERSION} AS build
WORKDIR /web-front-end/angular
ENV NG_CLI_ANALYTICS=false
COPY package.json package-lock.jso[n] ./
# package-lock.json is gitignored in this repo, so fall back to `npm install`
# when no lockfile is present in the build context.
RUN if [ -f package-lock.json ]; then npm ci --ignore-scripts; else npm install --ignore-scripts; fi
COPY . .
RUN npm run build

FROM node:${NODE_VERSION} AS runtime
WORKDIR /web-front-end/angular
ENV NG_CLI_ANALYTICS=false
COPY --from=build /web-front-end/angular /web-front-end/angular
EXPOSE 18093
ENTRYPOINT ["npm", "run", "start-prod"]
