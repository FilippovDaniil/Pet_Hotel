// Конфигурация Vite — инструмента сборки для frontend.
import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'  // плагин: JSX transform + Fast Refresh (горячая перезагрузка)

export default defineConfig({
  plugins: [react()],   // подключаем React-поддержку (без этого JSX не компилируется)
  server: {
    port: 3001,         // порт dev-сервера: http://localhost:3001 (чтобы не конфликтовать с backend)
    proxy: {
      '/api': {
        // Все запросы /api/* в dev-режиме проксируются на api-gateway.
        // В production (nginx.conf) аналогичный прокси настроен в nginx.
        target: 'http://localhost:8080',
        changeOrigin: true,  // меняет Host заголовок на target (нужно для виртуальных хостов)
      },
    },
  },
})
