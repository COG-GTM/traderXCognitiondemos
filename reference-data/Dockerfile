FROM node:22-bookworm-slim

WORKDIR /reference-data

COPY package.json package-lock.json ./
RUN npm ci

COPY . .
RUN npm run build

EXPOSE 18085

CMD ["npm", "run", "start:prod"]
