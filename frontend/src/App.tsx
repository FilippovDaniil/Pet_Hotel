// Корневой компонент приложения: настройка маршрутизации и guards доступа.
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import { useAuthStore } from './store/auth.store'         // Zustand store с token/role
import Layout from './components/layout/Layout'           // Navbar + <Outlet /> (общая обёртка)

// Импорты страниц по разделам
import LoginPage from './pages/auth/LoginPage'
import RegisterPage from './pages/auth/RegisterPage'
import LandingPage from './pages/LandingPage'
import DashboardPage from './pages/DashboardPage'
import RoomsPage from './pages/customer/RoomsPage'
import BookingCreatePage from './pages/customer/BookingCreatePage'
import MyBookingsPage from './pages/customer/MyBookingsPage'
import MenuPage from './pages/customer/MenuPage'
import MyOrdersPage from './pages/customer/MyOrdersPage'
import InvoicesPage from './pages/customer/InvoicesPage'
import ServicesPage from './pages/customer/ServicesPage'
import BookingDetailPage from './pages/BookingDetailPage'
import AllBookingsPage from './pages/reception/AllBookingsPage'
import ManageRoomsPage from './pages/admin/ManageRoomsPage'
import ManageMenuPage from './pages/admin/ManageMenuPage'
import ManageAmenitiesPage from './pages/admin/ManageAmenitiesPage'
import SupportPage from './pages/customer/SupportPage'
import AdminSupportPage from './pages/admin/AdminSupportPage'

// Guard: проверяет, авторизован ли пользователь.
// Если нет токена — редиректит на /login (replace = не добавляет в историю браузера).
// children — любые вложенные JSX-элементы (Route, компоненты).
function RequireAuth({ children }: { children: React.ReactNode }) {
  const isAuthenticated = useAuthStore((s) => s.isAuthenticated)  // селектор Zustand — реактивно обновляется
  if (!isAuthenticated) return <Navigate to="/login" replace />
  return <>{children}</>                                           // React.Fragment — не добавляет лишний DOM-элемент
}

// Guard: проверяет, входит ли роль текущего пользователя в список допустимых.
// roles: string[] — массив разрешённых ролей, например ['ADMIN'] или ['CUSTOMER', 'RECEPTION'].
function RequireRole({
  roles,
  children,
}: {
  roles: string[]
  children: React.ReactNode
}) {
  const role = useAuthStore((s) => s.role)          // текущая роль из Zustand (CUSTOMER/RECEPTION/ADMIN)
  if (!role || !roles.includes(role)) {
    // Роль не входит в список допустимых — показываем заглушку вместо редиректа
    return (
      <div className="flex items-center justify-center min-h-[40vh]">
        <div className="card text-center max-w-sm">
          <p className="text-4xl mb-4">🚫</p>
          <h2 className="text-xl font-bold text-gray-900 mb-2">Доступ запрещён</h2>
          <p className="text-gray-500">У вас нет прав для просмотра этой страницы.</p>
        </div>
      </div>
    )
  }
  return <>{children}</>
}

export default function App() {
  return (
    // BrowserRouter: использует HTML5 History API (URL без #hash).
    // Оборачивает всё приложение — делает доступными хуки useNavigate, useParams и т.д.
    <BrowserRouter>
      <Routes>
        {/* Публичные маршруты — доступны без авторизации */}
        <Route path="/" element={<LandingPage />} />
        <Route path="/login" element={<LoginPage />} />
        <Route path="/register" element={<RegisterPage />} />

        {/* Группа защищённых маршрутов: Layout с Navbar обёрнут в RequireAuth.
            Если пользователь не авторизован — все эти пути → /login.
            element без path: не добавляет сегмент URL, только рендерит обёртку. */}
        <Route
          element={
            <RequireAuth>
              <Layout />   {/* <Outlet /> внутри Layout подставляет дочерний маршрут */}
            </RequireAuth>
          }
        >
          {/* Дашборд — доступен всем авторизованным ролям */}
          <Route path="/dashboard" element={<DashboardPage />} />

          {/* /rooms — каталог номеров: клиент смотрит, admin управляет */}
          <Route
            path="/rooms"
            element={
              <RequireRole roles={['CUSTOMER', 'ADMIN']}>
                <RoomsPage />
              </RequireRole>
            }
          />
          {/* /bookings/new — форма создания бронирования (только CUSTOMER) */}
          <Route
            path="/bookings/new"
            element={
              <RequireRole roles={['CUSTOMER']}>
                <BookingCreatePage />
              </RequireRole>
            }
          />
          {/* /bookings/my — список бронирований текущего клиента */}
          <Route
            path="/bookings/my"
            element={
              <RequireRole roles={['CUSTOMER']}>
                <MyBookingsPage />
              </RequireRole>
            }
          />
          {/* /bookings/all — все бронирования системы (RECEPTION/ADMIN) */}
          <Route
            path="/bookings/all"
            element={
              <RequireRole roles={['RECEPTION', 'ADMIN']}>
                <AllBookingsPage />
              </RequireRole>
            }
          />
          {/* /bookings/:id — детальная страница бронирования.
              :id — динамический параметр, доступен через useParams().
              Нет RequireRole: доступна всем авторизованным (API сам ограничит по роли). */}
          <Route path="/bookings/:id" element={<BookingDetailPage />} />
          {/* /services — каталог дополнительных услуг (просмотр для всех ролей) */}
          <Route
            path="/services"
            element={
              <RequireRole roles={['CUSTOMER', 'RECEPTION', 'ADMIN']}>
                <ServicesPage />
              </RequireRole>
            }
          />
          {/* /menu — страница заказа еды из буфета (только CUSTOMER) */}
          <Route
            path="/menu"
            element={
              <RequireRole roles={['CUSTOMER']}>
                <MenuPage />
              </RequireRole>
            }
          />
          {/* /orders/my — история заказов буфета текущего клиента */}
          <Route
            path="/orders/my"
            element={
              <RequireRole roles={['CUSTOMER']}>
                <MyOrdersPage />
              </RequireRole>
            }
          />
          {/* /invoices — счета текущего клиента */}
          <Route
            path="/invoices"
            element={
              <RequireRole roles={['CUSTOMER']}>
                <InvoicesPage />
              </RequireRole>
            }
          />
          {/* /rooms/manage — CRUD управление номерами (только ADMIN) */}
          <Route
            path="/rooms/manage"
            element={
              <RequireRole roles={['ADMIN']}>
                <ManageRoomsPage />
              </RequireRole>
            }
          />
          {/* /menu/manage — CRUD управление меню буфета (только ADMIN) */}
          <Route
            path="/menu/manage"
            element={
              <RequireRole roles={['ADMIN']}>
                <ManageMenuPage />
              </RequireRole>
            }
          />
          {/* /amenities/manage — CRUD управление доп. услугами (только ADMIN) */}
          <Route
            path="/amenities/manage"
            element={
              <RequireRole roles={['ADMIN']}>
                <ManageAmenitiesPage />
              </RequireRole>
            }
          />
          {/* /support — чат клиента с поддержкой */}
          <Route
            path="/support"
            element={
              <RequireRole roles={['CUSTOMER']}>
                <SupportPage />
              </RequireRole>
            }
          />
          {/* /support/admin — панель администратора: список всех диалогов */}
          <Route
            path="/support/admin"
            element={
              <RequireRole roles={['ADMIN']}>
                <AdminSupportPage />
              </RequireRole>
            }
          />
        </Route>

        {/* Catch-all: любой неизвестный URL → главная страница */}
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </BrowserRouter>
  )
}
