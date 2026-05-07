# ── Стадия 1: сборка React-приложения ────────────────────────────────────────
# node:20-alpine: Node.js LTS на alpine — минимальный образ для сборки
FROM node:20-alpine AS builder
WORKDIR /app
# Сначала копируем только package.json → Docker кэширует слой установки зависимостей.
# Если изменились только src-файлы, npm install не перезапускается.
COPY package.json ./
RUN npm install
# Копируем исходники после установки зависимостей (оптимизация кэша)
COPY . .
# Vite собирает статику в /app/dist (JS, CSS, index.html)
RUN npm run build

# ── Стадия 2: production-сервер ──────────────────────────────────────────────
# nginx:alpine: статический HTTP-сервер; node и исходники не попадают в финальный образ
FROM nginx:alpine
# Копируем собранную статику в корень nginx
COPY --from=builder /app/dist /usr/share/nginx/html
# nginx.conf настраивает: try_files для SPA-роутинга + proxy_pass /api/* → api-gateway:8080
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
# daemon off: nginx работает на переднем плане — Docker видит процесс живым
CMD ["nginx", "-g", "daemon off;"]
