# ==========================================
# Stage 1: React Build
# ==========================================

FROM node:24-alpine AS build

WORKDIR /app

COPY package*.json ./

RUN npm ci

COPY . .

ARG VITE_API_BASE_URL=""
ENV VITE_API_BASE_URL=$VITE_API_BASE_URL

RUN npm run build


# ==========================================
# Stage 2: Non-root Nginx Runtime
# ==========================================

FROM nginx:1.29-alpine

COPY nginx.conf /etc/nginx/conf.d/default.conf

COPY --from=build /app/dist /usr/share/nginx/html

# Prepare writable directories for Nginx
RUN mkdir -p /var/cache/nginx /tmp \
    && chown -R nginx:nginx /var/cache/nginx /tmp \
    && sed -i \
       -e 's|pid /run/nginx.pid;|pid /tmp/nginx.pid;|' \
       -e 's|^user nginx;|# user nginx;|' \
       /etc/nginx/nginx.conf

# Run as non-root
USER nginx

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=10s --retries=3 \
    CMD wget -q --spider http://127.0.0.1:8080/health || exit 1

CMD ["nginx", "-g", "daemon off;"]