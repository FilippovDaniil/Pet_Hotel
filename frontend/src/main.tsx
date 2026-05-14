// Точка входа React-приложения — вызывается единожды при загрузке страницы.
import React from 'react'
import ReactDOM from 'react-dom/client'
import App from './App'
import './index.css'  // глобальные Tailwind-стили и кастомные CSS-классы (.btn-primary, .input и др.)

// ReactDOM.createRoot: React 18 Concurrent Mode API (заменил ReactDOM.render из React 17).
// getElementById('root')!: '!' — TypeScript non-null assertion, элемент гарантированно есть в index.html.
ReactDOM.createRoot(document.getElementById('root')!).render(
  // React.StrictMode: в development-режиме двойной рендер компонентов для обнаружения side-эффектов.
  // В production StrictMode не добавляет накладных расходов.
  <React.StrictMode>
    <App />
  </React.StrictMode>,
)
