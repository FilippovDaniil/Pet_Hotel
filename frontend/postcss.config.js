// PostCSS — инструмент обработки CSS, через который Vite прогоняет стили.
// Плагины применяются последовательно: сначала tailwindcss, потом autoprefixer.
export default {
  plugins: {
    // tailwindcss: генерирует утилитарные CSS-классы из tailwind.config.js.
    // Читает index.css с директивами @tailwind base/components/utilities.
    tailwindcss: {},
    // autoprefixer: автоматически добавляет vendor-prefixes (-webkit-, -moz-) для совместимости.
    // Например, display:grid → -ms-grid:... для старых браузеров.
    autoprefixer: {},
  },
}
