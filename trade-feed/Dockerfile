FROM node:22-bookworm-slim

WORKDIR /trade-feed
COPY package.json package-lock.json ./
RUN npm ci --omit=dev
COPY . .
EXPOSE 18086

ENTRYPOINT ["node", "index.js"]
