import { useState, useEffect } from 'react'
import { Link } from 'react-router-dom'
import { billingApi } from '../../api/billing.api'
import type { Invoice } from '../../types'
import { INVOICE_STATUS_LABELS } from '../../types'

function Spinner() {
  return (
    <div className="flex justify-center py-12">
      <div className="animate-spin border-4 border-primary-600 border-t-transparent rounded-full w-8 h-8" />
    </div>
  )
}

export default function InvoicesPage() {
  const [invoices, setInvoices] = useState<Invoice[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    billingApi
      .getMyInvoices()
      .then((data) => {
        setInvoices(data)
        setLoading(false)
      })
      .catch(() => {
        setError('Не удалось загрузить счета')
        setLoading(false)
      })
  }, [])

  const totalPaid = invoices
    .filter((i) => i.status === 'PAID')
    .reduce((sum, i) => sum + i.totalAmount, 0)

  const totalUnpaid = invoices
    .filter((i) => i.status === 'UNPAID')
    .reduce((sum, i) => sum + i.totalAmount, 0)

  if (loading) return <Spinner />

  return (
    <div>
      <h1 className="page-title">Мои счета</h1>

      {error && (
        <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-lg mb-4 text-sm">
          {error}
        </div>
      )}

      {/* Summary cards */}
      {invoices.length > 0 && (
        <div className="grid grid-cols-1 sm:grid-cols-3 gap-4 mb-6">
          <div className="card flex items-center gap-4">
            <div className="w-10 h-10 bg-blue-50 rounded-xl flex items-center justify-center text-xl flex-shrink-0">
              🧾
            </div>
            <div>
              <p className="text-sm text-gray-500">Всего счетов</p>
              <p className="text-2xl font-bold text-gray-900">{invoices.length}</p>
            </div>
          </div>
          <div className="card flex items-center gap-4">
            <div className="w-10 h-10 bg-green-50 rounded-xl flex items-center justify-center text-xl flex-shrink-0">
              ✅
            </div>
            <div>
              <p className="text-sm text-gray-500">Оплачено</p>
              <p className="text-2xl font-bold text-green-700">
                {totalPaid.toLocaleString('ru-RU')} ₽
              </p>
            </div>
          </div>
          <div className="card flex items-center gap-4">
            <div className="w-10 h-10 bg-red-50 rounded-xl flex items-center justify-center text-xl flex-shrink-0">
              ⏳
            </div>
            <div>
              <p className="text-sm text-gray-500">К оплате</p>
              <p className="text-2xl font-bold text-red-700">
                {totalUnpaid.toLocaleString('ru-RU')} ₽
              </p>
            </div>
          </div>
        </div>
      )}

      {invoices.length === 0 && !error && (
        <div className="text-center py-16 text-gray-500">
          <p className="text-5xl mb-4">🧾</p>
          <p className="text-lg font-medium">У вас пока нет счетов</p>
        </div>
      )}

      <div className="space-y-4">
        {invoices.map((invoice) => (
          <div key={invoice.id} className="card hover:shadow-md transition-shadow">
            <div className="flex flex-wrap items-start justify-between gap-3">
              <div>
                <div className="flex items-center gap-3 mb-3">
                  <h3 className="font-bold text-gray-900">Счёт #{invoice.id}</h3>
                  <span
                    className={`px-2.5 py-0.5 rounded-full text-xs font-semibold ${
                      invoice.status === 'PAID'
                        ? 'bg-green-100 text-green-700'
                        : 'bg-red-100 text-red-700'
                    }`}
                  >
                    {INVOICE_STATUS_LABELS[invoice.status]}
                  </span>
                </div>

                <div className="grid grid-cols-2 sm:grid-cols-4 gap-x-6 gap-y-1 text-sm">
                  <div>
                    <span className="text-gray-400">Проживание:</span>{' '}
                    <strong className="text-gray-900">
                      {invoice.roomAmount.toLocaleString('ru-RU')} ₽
                    </strong>
                  </div>
                  <div>
                    <span className="text-gray-400">Услуги:</span>{' '}
                    <strong className="text-gray-900">
                      {invoice.amenitiesAmount.toLocaleString('ru-RU')} ₽
                    </strong>
                  </div>
                  <div>
                    <span className="text-gray-400">Питание:</span>{' '}
                    <strong className="text-gray-900">
                      {invoice.diningAmount.toLocaleString('ru-RU')} ₽
                    </strong>
                  </div>
                  <div>
                    <span className="text-gray-400">Итого:</span>{' '}
                    <strong className="text-primary-700 text-base">
                      {invoice.totalAmount.toLocaleString('ru-RU')} ₽
                    </strong>
                  </div>
                </div>
              </div>

              <Link
                to={`/bookings/${invoice.bookingId}`}
                className="btn-secondary text-sm flex-shrink-0"
              >
                Подробности брони →
              </Link>
            </div>
          </div>
        ))}
      </div>
    </div>
  )
}
