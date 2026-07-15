# Strapi v5 CMS — builds the admin panel, runs in production mode.
# bookworm-slim (glibc) so better-sqlite3 uses its prebuilt binaries.
FROM node:22-bookworm-slim

WORKDIR /app
ENV NODE_ENV=production

COPY package.json package-lock.json ./
RUN npm ci

COPY . .
RUN npm run build

EXPOSE 1337
CMD ["npm", "run", "start"]
