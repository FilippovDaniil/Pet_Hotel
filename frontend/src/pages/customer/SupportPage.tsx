// Страница чата с поддержкой (CUSTOMER).
// Реализует polling каждые 15 секунд для получения новых ответов от admin.
import React, { useEffect, useRef, useState } from 'react'
import { supportApi } from '../../api/support.api'
import type { SupportMessage } from '../../types'

// Форматирует ISO datetime: "01.08, 14:30" — краткий формат для чата.
function formatTime(iso: string) {
  const d = new Date(iso)
  return d.toLocaleString('ru-RU', {
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

export default function SupportPage() {
  const [messages, setMessages] = useState<SupportMessage[]>([])
  const [loading, setLoading] = useState(true)
  const [sending, setSending] = useState(false)
  const [text, setText] = useState('')
  const [error, setError] = useState('')
  const bottomRef = useRef<HTMLDivElement>(null)        // якорь для прокрутки к последнему сообщению
  const textareaRef = useRef<HTMLTextAreaElement>(null)  // фокус после отправки

  // Загружает сообщения и помечает ответы admin как прочитанные.
  // silent=true: не показывает spinner при фоновом polling.
  const load = async (silent = false) => {
    if (!silent) setLoading(true)
    try {
      const data = await supportApi.getMyMessages()
      setMessages(data)
      await supportApi.markAsRead()  // сбрасываем счётчик непрочитанных (badge в navbar)
    } catch {
      if (!silent) setError('Не удалось загрузить сообщения')
    } finally {
      if (!silent) setLoading(false)
    }
  }

  useEffect(() => {
    load()
    // Polling: каждые 15 секунд проверяем новые сообщения без WebSocket.
    // 15_000 — числовой литерал с разделителем (ES2021), то же что 15000.
    const interval = setInterval(() => load(true), 15_000)
    return () => clearInterval(interval)  // очищаем при размонтировании компонента
  }, [])

  // Автоскролл вниз при появлении новых сообщений.
  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth' })
  }, [messages])

  const handleSend = async () => {
    const content = text.trim()
    if (!content) return
    setSending(true)
    setError('')
    try {
      const msg = await supportApi.sendMessage(content)
      setMessages((prev) => [...prev, msg])  // оптимистично добавляем в локальный список
      setText('')
      textareaRef.current?.focus()
    } catch {
      setError('Не удалось отправить сообщение')
    } finally {
      setSending(false)
    }
  }

  // Enter = отправить, Shift+Enter = новая строка в textarea.
  const handleKeyDown = (e: React.KeyboardEvent<HTMLTextAreaElement>) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault()
      handleSend()
    }
  }

  return (
    // calc(100vh - 160px): занимает всю высоту экрана за вычетом header/footer
    <div className="max-w-2xl mx-auto flex flex-col" style={{ height: 'calc(100vh - 160px)', minHeight: 400 }}>
      {/* Заголовок чата */}
      <div className="flex items-center gap-3 mb-4">
        <div className="w-10 h-10 rounded-full bg-primary-100 flex items-center justify-center text-xl">
          💬
        </div>
        <div>
          <h1 className="text-lg font-bold text-gray-900">Техническая поддержка</h1>
          <p className="text-xs text-gray-500">Мы ответим как можно скорее</p>
        </div>
      </div>

      {/* Область сообщений: flex-1 + overflow-y-auto = скролл только внутри */}
      <div className="flex-1 card p-4 overflow-y-auto flex flex-col gap-3 min-h-0">
        {loading && <Spinner />}

        {/* Пустое состояние */}
        {!loading && messages.length === 0 && (
          <div className="flex-1 flex flex-col items-center justify-center text-gray-400">
            <p className="text-5xl mb-3">👋</p>
            <p className="font-medium text-gray-600">Напишите нам!</p>
            <p className="text-sm mt-1">Ваш вопрос увидит администратор отеля</p>
          </div>
        )}

        {messages.map((msg) => {
          const isOwn = msg.senderRole === 'CUSTOMER'  // true = наше сообщение (справа)
          return (
            <div
              key={msg.id}
              className={`flex ${isOwn ? 'justify-end' : 'justify-start'}`}
            >
              <div className={`max-w-[75%] ${isOwn ? 'items-end' : 'items-start'} flex flex-col gap-1`}>
                {/* Подпись "Поддержка" только для сообщений admin */}
                {!isOwn && (
                  <span className="text-xs font-medium text-primary-700 px-1">Поддержка</span>
                )}
                {/* Пузырь: синий (наш) или серый (admin).
                    rounded-br-sm/rounded-bl-sm: "хвостик" у нужного угла */}
                <div
                  className={`px-4 py-2.5 rounded-2xl text-sm whitespace-pre-wrap break-words ${
                    isOwn
                      ? 'bg-primary-600 text-white rounded-br-sm'
                      : 'bg-gray-100 text-gray-800 rounded-bl-sm'
                  }`}
                >
                  {msg.content}
                </div>
                <span className="text-xs text-gray-400 px-1">
                  {formatTime(msg.createdAt)}
                  {/* Галочки статуса прочтения (только для наших сообщений):
                      ✓ = отправлено, ✓✓ = admin прочитал */}
                  {isOwn && (
                    <span className="ml-1">
                      {msg.readByAdmin ? ' ✓✓' : ' ✓'}
                    </span>
                  )}
                </span>
              </div>
            </div>
          )
        })}

        {/* Якорь для scrollIntoView */}
        <div ref={bottomRef} />
      </div>

      {error && (
        <div className="mt-2 text-sm text-red-600 bg-red-50 border border-red-200 px-3 py-2 rounded-lg">
          {error}
        </div>
      )}

      {/* Поле ввода сообщения */}
      <div className="mt-3 flex gap-2 items-end">
        <textarea
          ref={textareaRef}
          className="input flex-1 resize-none"
          rows={2}
          placeholder="Введите сообщение... (Enter — отправить, Shift+Enter — новая строка)"
          value={text}
          onChange={(e) => setText(e.target.value)}
          onKeyDown={handleKeyDown}
          disabled={sending}
          maxLength={2000}
        />
        <button
          className="btn-primary px-5 py-3 self-end"
          onClick={handleSend}
          disabled={sending || !text.trim()}  // disabled если пусто или отправляется
        >
          {sending ? '...' : 'Отправить'}
        </button>
      </div>
      {/* Счётчик символов */}
      <p className="text-xs text-gray-400 mt-1 text-right">{text.length}/2000</p>
    </div>
  )
}
