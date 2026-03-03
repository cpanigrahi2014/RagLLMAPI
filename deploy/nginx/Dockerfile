# ─── Stage 1: Build Frontend ─────────────────────────
FROM node:20-alpine AS builder

WORKDIR /app
COPY frontend/package*.json ./
RUN npm ci
COPY frontend/ ./

# Production build — API calls go to /api/v1 (same origin, proxied by Nginx)
ENV VITE_API_BASE_URL=/api/v1
ENV VITE_APP_NAME="CBSE AI Tutor"
RUN npm run build

# ─── Stage 2: Nginx ─────────────────────────────────────
FROM nginx:1.25-alpine

# Remove default nginx config
RUN rm /etc/nginx/conf.d/default.conf

# Copy custom config
COPY deploy/nginx/nginx.conf /etc/nginx/conf.d/default.conf

# Copy built frontend
COPY --from=builder /app/dist /usr/share/nginx/html

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
