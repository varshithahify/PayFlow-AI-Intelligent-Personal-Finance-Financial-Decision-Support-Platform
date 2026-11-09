FROM node:24-alpine AS build

WORKDIR /app

COPY package*.json ./
RUN npm ci

COPY . .

ARG VITE_API_BASE_URL=""
ENV VITE_API_BASE_URL=$VITE_API_BASE_URL

RUN npm run build


# ==========================================
# Non-root Nginx Runtime
# ==========================================

FROM nginx:1.29-alpine

# Apply Alpine Linux security updates
RUN apk update \
    && apk upgrade --no-cache \
    && rm -rf /var/cache/apk/*

COPY nginx.conf /etc/nginx/conf.d/default.conf

COPY --from=build /app/dist /usr/share/nginx/html

# Configure writable directories and Nginx paths
RUN mkdir -p \
    /var/cache/nginx/client_temp \
    /var/cache/nginx/proxy_temp \
    /var/cache/nginx/fastcgi_temp \
    /var/cache/nginx/uwsgi_temp \
    /var/cache/nginx/scgi_temp \
    /tmp \
    && chown -R nginx:nginx /var/cache/nginx /tmp \
    && sed -i \
       -e 's|^[[:space:]]*user[[:space:]].*;|# user directive disabled for non-root|' \
       -e 's|^[[:space:]]*pid[[:space:]].*;|pid /tmp/nginx.pid;|' \
       -e 's|^[[:space:]]*error_log[[:space:]].*;|error_log /dev/stderr notice;|' \
       -e 's|^[[:space:]]*access_log[[:space:]].*;|access_log /dev/stdout;|' \
       -e 's|^[[:space:]]*client_body_temp_path[[:space:]].*;||' \
       /etc/nginx/nginx.conf

USER nginx

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=10s --retries=3 \
    CMD wget -q --spider http://127.0.0.1:8080/health || exit 1

CMD ["nginx", "-g", "daemon off;"]
