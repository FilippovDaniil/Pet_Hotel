// Страница управления чатом поддержки для администратора (ADMIN).
// Двухпанельный интерфейс: список диалогов слева, переписка справа.
import React, { useEffect, useRef, useState } from 'react'
import { supportApi } from '../../api/support.api'
import type { ConversationSummary, SupportMessage } from '../../types'

// Умный форматировщик времени для списка диалогов:
// если сообщение сегодня — только время (14:30), иначе дата (01.08).
function formatTime(iso: string) {
  const d = new Date(iso)
  const now = new Date()
  const isToday =
    d.getDate() === now.getDate() &&
    d.getMonth() === now.getMonth() &&
    d.getFullYear() === now.getFullYear()
  if (isToday) {
    return d.toLocaleTimeString('ru-RU', { hour: '2-digit', minute: '2-digit' })
  }
  return d.toLocaleDateString('ru-RU', { day: '2-digit', month: '2-digit' })
}

// Полный формат для пузырей чата: "01.08, 14:30".
function formatFullTime(iso: string) {
  return new Date(iso).toLocaleString('ru-RU', {
    day: '2-digit',
    month: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  })
}

function Spinner() {
  return (
    <div className="flex justify-center py-8">
      <div className="animate-spin border-4 border-primary-600 border-t-transparent rounded-full w-8 h-8" />
    </div>
  )
}

export default function AdminSupportPage() {
  const [conversations, setConversations] = useState<ConversationSummary[]>([])
  const [selected, setSelected] = useState<ConversationSummary | null>(null)  // активный диалог
  const [messages, setMessages] = useState<SupportMessage[]>([])
  const [loadingList, setLoadingList] = useState(true)
  const [loadingMessages, setLoadingMessages] = useState(false)
  const [sending, setSending] = useState(false)
  const [text, setText] = useState('')
  const [error, setError] = useState('')
  const bottomRef = useRef<HTMLDivElement>(null)
  const textareaRef = useRef<HTMLTextAreaElement>(null)

  // Загрузка списка диалогов. silent=true не показывает spinner (для polling).
  const loadConversations = async (silent = false) => {
    if (!silent) setLoadingList(true)
    try {
      const data = await supportApi.getConversations()
      setConversations(data)
    } catch {
      if (!silent) setError('Не удалось загрузить диалоги')
    } finally {
      if (!silent) setLoadingList(false)
    }
  }

  // Загрузка сообщений конкретного клиента + пометка как прочитанных.
  const loadMessages = async (customerId: number, silent = false) => {
    if (!silent) setLoadingMessages(true)
    try {
      const data = await supportApi.getConversation(customerId)
      setMessages(data)
      if (!silent) {
        // Помечаем прочитанными только при явном открытии диалога (не при polling).
        await supportApi.markConversationAsRead(customerId)
        // Обновляем счётчик в списке без полного рефетча.
        setConversations((prev) =>
          prev.map((c) => (c.customerId === customerId ? { ...c, unreadByAdmin: 0 } : c))
        )
      }
    } catch {
      if (!silent) setError('Не удалось загрузить сообщения')
    } finally {
      if (!silent) setLoadingMessages(false)
    }
  }

  // Polling списка диалогов — каждые 15 секунд обновляем счётчики непрочитанных.
  useEffect(() => {
    loadConversations()
    const interval = setInterval(() => {
      loadConversations(true)
    }, 15_000)
    return () => clearInterval(interval)
  }, [])

  // Polling сообщений активного диалога — каждые 15 секунд.
  // Зависит от selected: при смене диалога старый интервал очищается, создаётся новый.
  useEffect(() => {
    if (!selected) return
    const interval = setInterval(() => loadMessages(selected.customerId, true), 15_000)
    return () => clearInterval(interval)
  }, [selected])

  // Автоскролл к последнему сообщению при обновлении.
  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth' })
  }, [messages])

  // Выбор диалога: загружает сообщения и сбрасывает поле ввода.
  const handleSelect = async (conv: ConversationSummary) => {
    setSelected(conv)
    setText('')
    setError('')
    await loadMessages(conv.customerId)
  }

  const handleSend = async () => {
    if (!selected || !text.trim()) return
    setSending(true)
    setError('')
    try {
      const msg = await supportApi.replyToCustomer(selected.customerId, text.trim())
      setMessages((prev) => [...prev, msg])  // оптимистично добавляем ответ
      setText('')
      textareaRef.current?.focus()
      loadConversations(true)  // обновляем превью последнего сообщения в списке
    } catch {
      setError('Не удалось отправить сообщение')
    } finally {
      setSending(false)
    }
  }

  const handleKeyDown = (e: React.KeyboardEvent<HTMLTextAreaElement>) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault()
      handleSend()
    }
  }

  return (
    // Высота: весь viewport за вычетом header/footer; минимум 480px.
    <div className="flex gap-0 rounded-xl overflow-hidden border border-gray-200 bg-white shadow-sm"
         style={{ height: 'calc(100vh - 160px)', minHeight: 480 }}>

      {/* ── Левая панель: список диалогов ── */}
      <div className="w-72 flex-shrink-0 border-r border-gray-200 flex flex-col">
        <div className="px-4 py-3 border-b border-gray-100 bg-gray-50">
          <h2 className="font-semibold text-gray-800 text-sm">Диалоги</h2>
          {conversations.length > 0 && (
            <p className="text-xs text-gray-400 mt-0.5">{conversations.length} клиентов</p>
          )}
        </div>

        <div className="flex-1 overflow-y-auto">
          {loadingList && <Spinner />}

          {!loadingList && conversations.length === 0 && (
            <div className="flex flex-col items-center justify-center h-full text-gray-400 px-4 text-center">
              <p className="text-3xl mb-2">📭</p>
              <p className="text-sm">Нет обращений</p>
            </div>
          )}

          {conversations.map((conv) => {
            const isActive = selected?.customerId === conv.customerId
            const hasUnread = conv.unreadByAdmin > 0  // непрочитанные от клиента
            return (
              <button
                key={conv.customerId}
                // border-l-2 border-l-primary-500: синяя полоска у активного диалога
                className={`w-full px-4 py-3 text-left border-b border-gray-50 transition-colors hover:bg-gray-50 ${
                  isActive ? 'bg-primary-50 border-l-2 border-l-primary-500' : ''
                }`}
                onClick={() => handleSelect(conv)}
              >
                <div className="flex items-center justify-between mb-0.5">
                  <span className={`text-sm font-medium truncate ${isActive ? 'text-primary-700' : 'text-gray-800'}`}>
                    {conv.customerEmail}
                  </span>
                  <div className="flex items-center gap-1.5 flex-shrink-0 ml-1">
                    {/* Красный badge: количество непрочитанных */}
                    {hasUnread && (
                      <span className="bg-primary-600 text-white text-xs font-bold rounded-full w-5 h-5 flex items-center justify-center">
                        {conv.unreadByAdmin > 9 ? '9+' : conv.unreadByAdmin}
                      </span>
                    )}
                    <span className="text-xs text-gray-400">{formatTime(conv.lastMessageAt)}</span>
                  </div>
                </div>
                {/* Превью последнего сообщения: жирный если есть непрочитанные */}
                <p className={`text-xs truncate ${hasUnread ? 'font-medium text-gray-700' : 'text-gray-400'}`}>
                  {conv.lastMessage}
                </p>
              </button>
            )
          })}
        </div>
      </div>

      {/* ── Правая панель: переписка ── */}
      <div className="flex-1 flex flex-col min-w-0">
        {/* Заглушка если диалог не выбран */}
        {!selected ? (
          <div className="flex-1 flex flex-col items-center justify-center text-gray-400">
            <p className="text-5xl mb-3">💬</p>
            <p className="font-medium text-gray-600">Выберите диалог</p>
            <p className="text-sm mt-1">Слева список обращений от клиентов</p>
          </div>
        ) : (
          <>
            {/* Заголовок чата с email и ID клиента */}
            <div className="px-5 py-3 border-b border-gray-100 bg-gray-50 flex items-center gap-3">
              <div className="w-8 h-8 rounded-full bg-primary-100 flex items-center justify-center text-sm">
                👤
              </div>
              <div>
                <p className="text-sm font-semibold text-gray-800">{selected.customerEmail}</p>
                <p className="text-xs text-gray-400">ID: {selected.customerId}</p>
              </div>
            </div>

            {/* Сообщения */}
            <div className="flex-1 overflow-y-auto px-5 py-4 flex flex-col gap-3">
              {loadingMessages && <Spinner />}

              {!loadingMessages && messages.length === 0 && (
                <div className="flex-1 flex flex-col items-center justify-center text-gray-400">
                  <p className="text-sm">Сообщений нет</p>
                </div>
              )}

              {messages.map((msg) => {
                const isAdmin = msg.senderRole === 'ADMIN'  // true = наш ответ (справа)
                return (
                  <div
                    key={msg.id}
                    className={`flex ${isAdmin ? 'justify-end' : 'justify-start'}`}
                  >
                    <div className={`max-w-[70%] flex flex-col gap-1 ${isAdmin ? 'items-end' : 'items-start'}`}>
                      {/* Email клиента над его сообщениями */}
                      {!isAdmin && (
                        <span className="text-xs font-medium text-gray-500 px-1">
                          {msg.customerEmail}
                        </span>
                      )}
                      <div
                        className={`px-4 py-2.5 rounded-2xl text-sm whitespace-pre-wrap break-words ${
                          isAdmin
                            ? 'bg-primary-600 text-white rounded-br-sm'  // ответ admin — синий справа
                            : 'bg-gray-100 text-gray-800 rounded-bl-sm'  // сообщение клиента — серый слева
                        }`}
                      >
                        {msg.content}
                      </div>
                      <span className="text-xs text-gray-400 px-1">
                        {formatFullTime(msg.createdAt)}
                        {/* Галочки прочтения для ответов admin */}
                        {isAdmin && (
                          <span className="ml-1">{msg.readByCustomer ? ' ✓✓' : ' ✓'}</span>
                        )}
                      </span>
                    </div>
                  </div>
                )
              })}

              <div ref={bottomRef} />
            </div>

            {error && (
              <div className="mx-5 mb-2 text-sm text-red-600 bg-red-50 border border-red-200 px-3 py-2 rounded-lg">
                {error}
              </div>
            )}

            {/* Поле ответа */}
            <div className="px-5 py-3 border-t border-gray-100 flex gap-2 items-end">
              <textarea
                ref={textareaRef}
                className="input flex-1 resize-none"
                rows={2}
                placeholder="Введите ответ... (Enter — отправить)"
                value={text}
                onChange={(e) => setText(e.target.value)}
                onKeyDown={handleKeyDown}
                disabled={sending}
                maxLength={2000}
              />
              <button
                className="btn-primary px-5 py-3 self-end"
                onClick={handleSend}
                disabled={sending || !text.trim()}
              >
                {sending ? '...' : 'Ответить'}
              </button>
            </div>
          </>
        )}
      </div>
    </div>
  )
}
