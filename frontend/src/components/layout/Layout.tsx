import { NavLink, useNavigate, Outlet } from 'react-router-dom'
import { useAuthStore } from '../../store/auth.store'

const navLinks = {
  CUSTOMER: [
    { to: '/dashboard', label: 'Главная' },
    { to: '/rooms', label: 'Номера' },
    { to: '/services', label: 'Услуги' },
    { to: '/bookings/my', label: 'Мои брони' },
    { to: '/menu', label: 'Меню' },
    { to: '/orders/my', label: 'Заказы' },
    { to: '/invoices', label: 'Счета' },
    { to: '/support', label: 'Поддержка' },
  ],
  RECEPTION: [
    { to: '/dashboard', label: 'Главная' },
    { to: '/bookings/all', label: 'Все брони' },
    { to: '/services', label: 'Услуги' },
  ],
  ADMIN: [
    { to: '/dashboard', label: 'Главная' },
    { to: '/rooms/manage', label: 'Номера' },
    { to: '/bookings/all', label: 'Все брони' },
    { to: '/menu/manage', label: 'Меню' },
    { to: '/amenities/manage', label: 'Услуги' },
    { to: '/support/admin', label: 'Поддержка' },
  ],
}

export default function Layout() {
  const { role, email, logout } = useAuthStore()
  const navigate = useNavigate()

  const links = role ? navLinks[role] ?? [] : []

  const handleLogout = () => {
    logout()
    navigate('/login')
  }

  return (
    <div className="min-h-screen flex flex-col bg-gray-50">
      <header className="bg-white border-b border-gray-200 shadow-sm">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex items-center justify-between h-16">
            <div className="flex items-center gap-2">
              <span className="text-2xl">🏨</span>
              <span className="text-lg font-bold text-primary-700">Pet Hotel</span>
            </div>
            <nav className="hidden md:flex items-center gap-1">
              {links.map((link) => (
                <NavLink
                  key={link.to}
                  to={link.to}
                  className={({ isActive }) =>
                    `px-3 py-2 rounded-lg text-sm font-medium transition-colors ${
                      isActive
                        ? 'bg-primary-50 text-primary-700'
                        : 'text-gray-600 hover:bg-gray-100 hover:text-gray-900'
                    }`
                  }
                >
                  {link.label}
                </NavLink>
              ))}
            </nav>
            <div className="flex items-center gap-3">
              <span className="text-sm text-gray-500 hidden sm:block">{email}</span>
              <button
                onClick={handleLogout}
                className="btn-secondary text-sm"
              >
                Выйти
              </button>
            </div>
          </div>
        </div>
        {/* Mobile nav */}
        <div className="md:hidden border-t border-gray-100 px-4 py-2 flex gap-1 flex-wrap">
          {links.map((link) => (
            <NavLink
              key={link.to}
              to={link.to}
              className={({ isActive }) =>
                `px-3 py-1.5 rounded-lg text-xs font-medium transition-colors ${
                  isActive
                    ? 'bg-primary-50 text-primary-700'
                    : 'text-gray-600 hover:bg-gray-100'
                }`
              }
            >
              {link.label}
            </NavLink>
          ))}
        </div>
      </header>

      <main className="flex-1 max-w-7xl w-full mx-auto px-4 sm:px-6 lg:px-8 py-8">
        <Outlet />
      </main>

      <footer className="bg-white border-t border-gray-200 py-4 text-center text-sm text-gray-500">
        © {new Date().getFullYear()} Pet Hotel — Система управления отелем
      </footer>
    </div>
  )
}
