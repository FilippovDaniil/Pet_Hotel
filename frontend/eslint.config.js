// ESLint flat config (формат ESLint v9+): массив конфигурационных объектов.
// Flat config заменяет .eslintrc.json — все настройки в одном файле.
import js from '@eslint/js'                        // базовые правила JavaScript (no-unused-vars и др.)
import globals from 'globals'                       // наборы глобальных переменных (browser: window, document...)
import reactHooks from 'eslint-plugin-react-hooks'  // правила для хуков: exhaustive-deps, rules-of-hooks
import reactRefresh from 'eslint-plugin-react-refresh'  // предупреждает если компонент нельзя HMR-обновить
import tseslint from 'typescript-eslint'            // TypeScript-aware правила ESLint

export default tseslint.config(
  { ignores: ['dist'] },  // исключаем папку сборки — не линтим скомпилированный код
  {
    // Расширяем двумя наборами правил: JS-рекомендации + TypeScript-рекомендации.
    extends: [js.configs.recommended, ...tseslint.configs.recommended],
    files: ['**/*.{ts,tsx}'],  // применяем только к TypeScript-файлам
    languageOptions: {
      ecmaVersion: 2020,         // синтаксис ES2020 (optional chaining, nullish coalescing и т.д.)
      globals: globals.browser,  // предопределённые глобалы: window, document, fetch, localStorage...
    },
    plugins: {
      'react-hooks': reactHooks,
      'react-refresh': reactRefresh,
    },
    rules: {
      // Распространяем рекомендованные правила react-hooks (rules-of-hooks + exhaustive-deps).
      ...reactHooks.configs.recommended.rules,
      // react-refresh/only-export-components: предупреждает если файл экспортирует не только компоненты
      // (это нарушает HMR — hot module replacement не сможет обновить только компонент).
      // allowConstantExport: true разрешает экспортировать константы рядом с компонентом.
      'react-refresh/only-export-components': ['warn', { allowConstantExport: true }],
      // @typescript-eslint/no-unused-vars: ошибка при неиспользуемых переменных,
      // но игнорирует параметры начинающиеся с '_' (конвенция для намеренно неиспользуемых).
      '@typescript-eslint/no-unused-vars': ['error', { argsIgnorePattern: '^_' }],
    },
  },
)
