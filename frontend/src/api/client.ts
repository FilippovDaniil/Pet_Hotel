// Центральный Axios-клиент: единая точка для всех HTTP-запросов к бэкенду.
// Все api/*.api.ts импортируют именно этот client — не создают свой Axios-экземпляр.
import axios from 'axios'

// axios.create() — создаёт изолированный экземпляр с базовыми настройками.
// baseURL: '/api' — все запросы идут на /api/*, что Vite (dev) проксирует на localhost:8080,
//   а nginx (prod) проксирует на api-gateway:8080.
const client = axios.create({
  baseURL: '/api',
  headers: { 'Content-Type': 'application/json' },
})

// Interceptor запросов: добавляет JWT-токен в заголовок Authorization.
// Читает токен из localStorage — он сохраняется при login и удаляется при logout.
// Bearer-схема: сервер ожидает "Authorization: Bearer eyJ..."
client.interceptors.request.use((config) => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config // возвращаем конфиг (обязательно — иначе запрос не отправится)
})

// Interceptor ответов: обработка ошибок на уровне всего приложения.
// (response) => response — успешные ответы пропускаем без изменений.
client.interceptors.response.use(
  (response) => response,
  (error) => {
    // 401 Unauthorized: JWT истёк или недействителен → автоматический logout.
    // Чистим localStorage и перенаправляем на /login без вывода ошибки пользователю.
    if (error.response?.status === 401) {
      localStorage.removeItem('token')
      localStorage.removeItem('user')
      window.location.href = '/login' // жёсткий редирект (не React Router navigate)
    }
    // Promise.reject: пробрасываем ошибку дальше — вызывающий код может обработать её сам.
    return Promise.reject(error)
  }
)

export default client
