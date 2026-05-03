import React from 'react'
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import { useAuthStore } from './store/auth.store'
import Layout from './components/layout/Layout'

import LoginPage from './pages/auth/LoginPage'
import RegisterPage from './pages/auth/RegisterPage'
import DashboardPage from './pages/DashboardPage'
import RoomsPage from './pages/customer/RoomsPage'
import BookingCreatePage from './pages/customer/BookingCreatePage'
import MyBookingsPage from './pages/customer/MyBookingsPage'
import MenuPage from './pages/customer/MenuPage'
import InvoicesPage from './pages/customer/InvoicesPage'
import BookingDetailPage from './pages/BookingDetailPage'
import AllBookingsPage from './pages/reception/AllBookingsPage'
import ManageRoomsPage from './pages/admin/ManageRoomsPage'
import ManageMenuPage from './pages/admin/ManageMenuPage'
import ManageAmenitiesPage from './pages/admin/ManageAmenitiesPage'

function RequireAuth({ children }: { children: React.ReactNode }) {
  const isAuthenticated = useAuthStore((s) => s.isAuthenticated)
  if (!isAuthenticated) return <Navigate to="/login" replace />
  return <>{children}</>
}

function RequireRole({
  roles,
  children,
}: {
  roles: string[]
  children: React.ReactNode
}) {
  const role = useAuthStore((s) => s.role)
  if (!role || !roles.includes(role)) {
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

function RootRedirect() {
  const isAuthenticated = useAuthStore((s) => s.isAuthenticated)
  return <Navigate to={isAuthenticated ? '/dashboard' : '/login'} replace />
}

export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<RootRedirect />} />
        <Route path="/login" element={<LoginPage />} />
        <Route path="/register" element={<RegisterPage />} />

        <Route
          element={
            <RequireAuth>
              <Layout />
            </RequireAuth>
          }
        >
          <Route path="/dashboard" element={<DashboardPage />} />

          <Route
            path="/rooms"
            element={
              <RequireRole roles={['CUSTOMER', 'ADMIN']}>
                <RoomsPage />
              </RequireRole>
            }
          />
          <Route
            path="/bookings/new"
            element={
              <RequireRole roles={['CUSTOMER']}>
                <BookingCreatePage />
              </RequireRole>
            }
          />
          <Route
            path="/bookings/my"
            element={
              <RequireRole roles={['CUSTOMER']}>
                <MyBookingsPage />
              </RequireRole>
            }
          />
          <Route
            path="/bookings/all"
            element={
              <RequireRole roles={['RECEPTION', 'ADMIN']}>
                <AllBookingsPage />
              </RequireRole>
            }
          />
          <Route path="/bookings/:id" element={<BookingDetailPage />} />
          <Route
            path="/menu"
            element={
              <RequireRole roles={['CUSTOMER']}>
                <MenuPage />
              </RequireRole>
            }
          />
          <Route
            path="/invoices"
            element={
              <RequireRole roles={['CUSTOMER']}>
                <InvoicesPage />
              </RequireRole>
            }
          />
          <Route
            path="/rooms/manage"
            element={
              <RequireRole roles={['ADMIN']}>
                <ManageRoomsPage />
              </RequireRole>
            }
          />
          <Route
            path="/menu/manage"
            element={
              <RequireRole roles={['ADMIN']}>
                <ManageMenuPage />
              </RequireRole>
            }
          />
          <Route
            path="/amenities/manage"
            element={
              <RequireRole roles={['ADMIN']}>
                <ManageAmenitiesPage />
              </RequireRole>
            }
          />
        </Route>

        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </BrowserRouter>
  )
}
