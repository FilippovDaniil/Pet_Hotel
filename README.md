# 🏨 Pet Hotel — Микросервисный Backend + Frontend

[![CI](https://github.com/FilippovDaniil/Pet_Hotel/actions/workflows/ci.yml/badge.svg)](https://github.com/FilippovDaniil/Pet_Hotel/actions/workflows/ci.yml)
[![Docker Publish](https://github.com/FilippovDaniil/Pet_Hotel/actions/workflows/docker-publish.yml/badge.svg)](https://github.com/FilippovDaniil/Pet_Hotel/actions/workflows/docker-publish.yml)
[![CodeQL](https://github.com/FilippovDaniil/Pet_Hotel/actions/workflows/codeql.yml/badge.svg)](https://github.com/FilippovDaniil/Pet_Hotel/actions/workflows/codeql.yml)

Полнофункциональная система управления отелем: бронирование номеров, дополнительные услуги (баня, сауна, бассейн, бильярд, массаж), буфет с дневными лимитами, биллинг, ролевая модель доступа.

---

## Содержание

1. [Обзор проекта](#1-обзор-проекта)
2. [Архитектура](#2-архитектура)
3. [Технологический стек](#3-технологический-стек)
4. [Структура проекта](#4-структура-проекта)
5. [Быстрый старт (Docker Compose)](#5-быстрый-старт-docker-compose)
6. [Демо-пользователи](#6-демо-пользователи)
7. [Локальная разработка](#7-локальная-разработка)
8. [Переменные окружения](#8-переменные-окружения)
9. [API Reference](#9-api-reference)
10. [Kafka события](#10-kafka-события)
11. [Redis кэширование](#11-redis-кэширование)
12. [Безопасность и JWT](#12-безопасность-и-jwt)
13. [Привилегии по классам номеров](#13-привилегии-по-классам-номеров)
14. [Логирование и мониторинг](#14-логирование-и-мониторинг)
15. [Frontend](#15-frontend)
16. [Роли и права доступа](#16-роли-и-права-доступа)
17. [Устранение проблем](#17-устранение-проблем)

---

## 1. Обзор проекта

Pet Hotel — backend-приложение для управления гостиничным отелем с микросервисной архитектурой. Система поддерживает три роли пользователей, бронирование номеров и услуг, учёт питания с лимитами, автоматический биллинг и централизованное логирование.

### Ключевые возможности

- **Клиент**: поиск номеров, бронирование с выбором услуг, заказ еды из буфета, просмотр счетов
- **Ресепшн**: подтверждение броней, заселение/выселение, управление счетами
- **Администратор**: управление номерами, услугами, меню буфета
- **Автоматический биллинг**: формируется при выселении через Kafka-события
- **Redis**: кэш доступных номеров и дневные лимиты буфета с TTL до полуночи
- **Structured logging**: JSON-логи из всех сервисов → Promtail → Loki → Grafana

---

## 2. Архитектура

```
                              ┌──────────────────┐
                              │    Frontend      │
                              │  React + Vite    │
                              │   :80 (nginx)    │
                              └────────┬─────────┘
                                       │ HTTP /api/*
                              ┌────────▼─────────┐
                              │   API Gateway    │
                              │  Spring Cloud    │
                              │  Gateway :8080   │
                              │  JWT Filter      │
                              └──┬──┬──┬──┬──┬───┘
                    ┌────────────┘  │  │  │  └────────────────┐
                    │     ┌─────────┘  │  └──────────┐        │
                    ▼     ▼            ▼             ▼        ▼
             ┌──────────┐ ┌─────────┐ ┌──────────┐ ┌───────┐ ┌─────────┐
             │ customer │ │  room   │ │ booking  │ │dining │ │billing  │
             │ service  │ │ service │ │ service  │ │service│ │service  │
             │  :8081   │ │  :8082  │ │  :8083   │ │ :8085 │ │  :8086  │
             └──────────┘ └─────────┘ └────┬─────┘ └───────┘ └─────────┘
                                           │ WebClient
                                    ┌──────▼──────┐
                                    │  amenity    │
                                    │  service    │
                                    │   :8084     │
                                    └─────────────┘

  ┌──────────────────────────── Kafka ───────────────────────────────────┐
  │  booking.created → room-service (блокировка дат)                     │
  │  booking.created → billing-service (учёт)                            │
  │  booking.confirmed → уведомление                                     │
  │  booking.cancelled → room-service (разблокировка дат)                │
  │  booking.completed → billing-service (создание счёта)                │
  │  order.created → billing-service (добавление к счёту)                │
  │  service.booked → amenity-service (блокировка слота)                 │
  └──────────────────────────────────────────────────────────────────────┘

  Инфраструктура: PostgreSQL · Redis · Kafka+Zookeeper · Loki · Grafana
```

### Взаимодействие сервисов

| Источник | Цель | Метод | Когда |
|---|---|---|---|
| booking-service | room-service | HTTP WebClient | Получение информации о номере при бронировании |
| dining-service | booking-service | HTTP WebClient | Получение класса номера для расчёта лимитов |
| billing-service | Kafka (listener) | Kafka | Создание счёта при booking.completed |
| billing-service | Kafka (listener) | Kafka | Добавление расходов при order.created |
| room-service | Kafka (listener) | Kafka | Блокировка/разблокировка дат |

---

## 3. Технологический стек

### Backend

| Компонент | Технология |
|---|---|
| Язык | Java 17 |
| Фреймворк | Spring Boot 3.3.5 |
| Микросервисы | Spring Cloud 2023.0.3 |
| API Gateway | Spring Cloud Gateway |
| База данных | PostgreSQL 15 |
| Кэш | Redis 7 |
| Очередь сообщений | Apache Kafka (Confluent 7.5) |
| Безопасность | Spring Security + JWT (jjwt 0.12.3) |
| Логирование | SLF4J + Logback + logstash-logback-encoder |
| Документация API | SpringDoc OpenAPI (Swagger UI) |
| Сборка | Gradle 8 (multi-project) |

### Frontend

| Компонент | Технология |
|---|---|
| Фреймворк | React 18 + TypeScript 5 |
| Сборка | Vite 5 |
| Стили | TailwindCSS 3 |
| Роутинг | React Router DOM 6 |
| HTTP клиент | Axios |
| Состояние | Zustand |

### Инфраструктура

| Компонент | Технология |
|---|---|
| Контейнеризация | Docker + Docker Compose |
| Сбор логов | Grafana Promtail 2.9 |
| Хранение логов | Grafana Loki 2.9 |
| Визуализация | Grafana 10.2 |

---

## 4. Структура проекта

```
Pet_Hotel/
├── build.gradle                    # Корневой Gradle (java 17, common config)
├── settings.gradle                 # Список всех модулей
├── gradle/
│   └── libs.versions.toml          # Централизованный version catalog
├── init-db.sql                     # SQL: CREATE SCHEMA для каждого сервиса
├── docker-compose.yml              # Весь стек одной командой
├── promtail-config.yml             # Конфигурация сбора логов из Docker
│
├── common/                         # Общая библиотека (не Spring Boot app)
│   ├── build.gradle
│   └── src/main/java/com/pethotel/common/
│       ├── enums/
│       │   ├── Role.java           # CUSTOMER, RECEPTION, ADMIN
│       │   ├── RoomClass.java      # ORDINARY, MIDDLE, PREMIUM
│       │   ├── BookingStatus.java  # PENDING, CONFIRMED, CANCELLED, COMPLETED
│       │   └── ServiceType.java    # SAUNA, BATH, POOL, BILLIARD_RUS, BILLIARD_US, MASSAGE
│       ├── event/                  # Kafka события (DTO)
│       │   ├── BookingCreatedEvent.java
│       │   ├── BookingConfirmedEvent.java
│       │   ├── BookingCancelledEvent.java
│       │   ├── BookingCompletedEvent.java
│       │   ├── ServiceBookedEvent.java
│       │   └── OrderCreatedEvent.java
│       ├── kafka/
│       │   └── KafkaTopics.java    # Константы топиков
│       └── security/
│           └── JwtUtil.java        # Генерация и валидация JWT
│
├── customer-service/               # Порт 8081, схема БД: customer
│   ├── build.gradle
│   ├── Dockerfile
│   └── src/main/
│       ├── java/com/pethotel/customer/
│       │   ├── CustomerServiceApplication.java
│       │   ├── entity/Customer.java          # id, email, passwordHash, firstName, lastName, phone, role
│       │   ├── repository/CustomerRepository.java
│       │   ├── dto/                          # RegisterRequest, LoginRequest, AuthResponse, CustomerDto
│       │   ├── service/CustomerService.java  # register, login, getById, updateRole
│       │   ├── controller/
│       │   │   ├── AuthController.java       # POST /api/auth/register, /api/auth/login
│       │   │   └── CustomerController.java   # GET /api/customers, /api/customers/{id}
│       │   └── config/
│       │       ├── SecurityConfig.java       # BCryptPasswordEncoder, stateless
│       │       ├── AppConfig.java            # JwtUtil bean
│       │       └── GlobalExceptionHandler.java
│       └── resources/
│           ├── application.yml
│           └── logback-spring.xml
│
├── room-service/                   # Порт 8082, схема БД: room
│   ├── build.gradle
│   ├── Dockerfile
│   └── src/main/
│       ├── java/com/pethotel/room/
│       │   ├── RoomServiceApplication.java
│       │   ├── entity/
│       │   │   ├── Room.java                 # id, roomNumber, roomClass, capacity, pricePerNight
│       │   │   └── RoomUnavailableDate.java  # room_id, date (для проверки доступности)
│       │   ├── repository/
│       │   │   ├── RoomRepository.java       # findAvailableRooms(checkIn, checkOut, guests)
│       │   │   └── RoomUnavailableDateRepository.java
│       │   ├── dto/                          # RoomDto, RoomRequest, RoomAvailabilityRequest
│       │   ├── service/RoomService.java      # findAvailable (с @Cacheable), CRUD, blockDates, unblockDates
│       │   ├── controller/RoomController.java  # GET /search, CRUD /api/rooms
│       │   └── config/
│       │       ├── CacheConfig.java          # Redis: available-rooms TTL 5 мин
│       │       ├── KafkaConsumerConfig.java  # Слушает booking.created, booking.cancelled
│       │       └── SecurityConfig.java
│       └── resources/
│           ├── application.yml
│           └── logback-spring.xml
│
├── booking-service/                # Порт 8083, схема БД: booking
│   ├── build.gradle
│   ├── Dockerfile
│   └── src/main/
│       ├── java/com/pethotel/booking/
│       │   ├── BookingServiceApplication.java
│       │   ├── entity/
│       │   │   ├── Booking.java              # id, customerId, roomId, roomClass, checkIn/Out, status, totalPrice
│       │   │   └── BookingAmenity.java       # serviceType, startTime, endTime, price
│       │   ├── repository/
│       │   │   ├── BookingRepository.java    # findByCustomerId, findByStatus
│       │   │   └── BookingAmenityRepository.java  # findConflicting (проверка конфликтов)
│       │   ├── dto/                          # BookingRequest, BookingDto, AmenityDto, RoomDto
│       │   ├── client/RoomClient.java        # WebClient → GET room-service/api/rooms/{id}
│       │   ├── service/
│       │   │   ├── BookingService.java       # create, confirm, cancel, checkIn, checkOut
│       │   │   └── AmenityPriceCalculator.java  # расчёт цен по классу номера
│       │   ├── controller/BookingController.java
│       │   └── config/
│       │       ├── AppConfig.java            # WebClient.Builder bean
│       │       └── SecurityConfig.java
│       └── resources/
│           ├── application.yml
│           └── logback-spring.xml
│
├── amenity-service/                # Порт 8084, схема БД: amenity
│   ├── build.gradle
│   ├── Dockerfile
│   └── src/main/
│       ├── java/com/pethotel/amenity/
│       │   ├── AmenityServiceApplication.java
│       │   ├── entity/Amenity.java           # id, name, type, defaultPrice, maxDurationMinutes, description, available, imageData (bytea), imageMimeType
│       │   ├── repository/AmenityRepository.java
│       │   ├── dto/                          # AmenityDto (+ description, available, hasImage), AmenityRequest
│       │   ├── service/AmenityService.java   # CRUD, uploadImage (макс. 2МБ), getImage
│       │   ├── controller/AmenityController.java  # GET /api/amenities, CRUD (ADMIN), POST/GET /{id}/image
│       │   └── config/
│       │       ├── KafkaConsumerConfig.java  # Слушает service.booked
│       │       └── SecurityConfig.java
│       └── resources/
│           ├── application.yml
│           └── logback-spring.xml
│
├── dining-service/                 # Порт 8085, схема БД: dining
│   ├── build.gradle
│   ├── Dockerfile
│   └── src/main/
│       ├── java/com/pethotel/dining/
│       │   ├── DiningServiceApplication.java
│       │   ├── entity/
│       │   │   ├── MenuItem.java             # id, name, price, category, available
│       │   │   ├── Order.java                # id, bookingId, customerId, menuItemId, menuItemName, qty, totalAmount, paidByLimit, extraCharge, deliveryType
│       │   │   └── DeliveryType.java         # ROOM_DELIVERY, DINING_ROOM
│       │   ├── repository/
│       │   │   ├── MenuItemRepository.java
│       │   │   └── OrderRepository.java      # findByBookingId, findByCustomerIdOrderByOrderTimeDesc
│       │   ├── dto/                          # MenuItemDto, MenuItemRequest, OrderDto (+ menuItemName, deliveryType), OrderRequest (+ deliveryType)
│       │   ├── service/
│       │   │   ├── DailyLimitService.java    # Redis: ключ dining:limit:{bookingId}:{date}, TTL до полуночи
│       │   │   ├── MenuService.java          # CRUD + @Cacheable
│       │   │   └── OrderService.java         # создание заказа, расчёт лимитов, Kafka event, getByCustomerId
│       │   ├── controller/
│       │   │   ├── MenuController.java       # GET /api/menu, CRUD (ADMIN)
│       │   │   └── OrderController.java      # POST /api/orders, GET /api/orders/my, GET /api/orders/booking/{id}
│       │   └── config/
│       │       ├── AppConfig.java            # WebClient.Builder
│       │       ├── CacheConfig.java          # Redis: menu-items TTL 1 час
│       │       └── SecurityConfig.java
│       └── resources/
│           ├── application.yml
│           └── logback-spring.xml
│
├── billing-service/                # Порт 8086, схема БД: billing
│   ├── build.gradle
│   ├── Dockerfile
│   └── src/main/
│       ├── java/com/pethotel/billing/
│       │   ├── BillingServiceApplication.java
│       │   ├── entity/
│       │   │   ├── Invoice.java              # id, bookingId, customerId, roomAmount, amenitiesAmount, diningAmount, total, status
│       │   │   └── InvoiceStatus.java        # UNPAID, PAID
│       │   ├── repository/InvoiceRepository.java  # findByBookingId, findByCustomerId
│       │   ├── dto/                          # InvoiceDto, PaymentRequest
│       │   ├── service/BillingService.java   # createInvoice, addDiningCharge, pay
│       │   ├── controller/InvoiceController.java  # GET /api/invoices/my, /booking/{id}, POST /{id}/pay
│       │   └── config/
│       │       ├── KafkaConsumerConfig.java  # Слушает booking.completed, order.created
│       │       └── SecurityConfig.java
│       └── resources/
│           ├── application.yml
│           └── logback-spring.xml
│
├── api-gateway/                    # Порт 8080
│   ├── build.gradle
│   ├── Dockerfile
│   └── src/main/
│       ├── java/com/pethotel/gateway/
│       │   ├── ApiGatewayApplication.java
│       │   ├── filter/JwtAuthFilter.java     # GlobalFilter: валидация JWT, X-User-Id/Role заголовки
│       │   └── config/AppConfig.java         # JwtUtil bean
│       └── resources/
│           ├── application.yml               # Маршруты ко всем сервисам
│           └── logback-spring.xml
│
└── frontend/                       # React + TypeScript
    ├── package.json
    ├── vite.config.ts              # dev proxy /api → localhost:8080
    ├── tsconfig.json
    ├── tailwind.config.js
    ├── postcss.config.js
    ├── index.html
    ├── nginx.conf                  # production: proxy /api → api-gateway:8080
    ├── Dockerfile                  # multi-stage: node:20 build → nginx:alpine
    └── src/
        ├── main.tsx
        ├── App.tsx                 # Router + route guards (RequireAuth, RequireRole)
        ├── index.css               # Tailwind + компонентные классы
        ├── types/                  # TypeScript интерфейсы
        │   ├── auth.ts             # Role, AuthResponse, Customer
        │   ├── room.ts             # RoomClass, Room, RoomRequest
        │   ├── booking.ts          # BookingStatus, ServiceType, Booking, BookingRequest
        │   ├── amenity.ts          # Amenity, AmenityRequest
        │   ├── dining.ts           # MenuItem, Order, OrderRequest
        │   ├── billing.ts          # Invoice, InvoiceStatus
        │   └── index.ts            # Barrel re-export
        ├── api/                    # HTTP-клиенты для каждого сервиса
        │   ├── client.ts           # Axios: базовый URL, Bearer token interceptor, 401 redirect
        │   ├── auth.api.ts
        │   ├── room.api.ts
        │   ├── booking.api.ts
        │   ├── amenity.api.ts
        │   ├── dining.api.ts
        │   └── billing.api.ts
        ├── store/
        │   └── auth.store.ts       # Zustand: token, userId, role, login(), logout()
        ├── components/
        │   └── layout/
        │       └── Layout.tsx      # Navbar + Sidebar + Outlet (роль-зависимые ссылки)
        └── pages/
            ├── LandingPage.tsx     # Публичная главная: hero, статистика, услуги из API
            ├── auth/
            │   ├── LoginPage.tsx
            │   └── RegisterPage.tsx
            ├── DashboardPage.tsx   # Разный контент для каждой роли
            ├── BookingDetailPage.tsx  # Детали брони + действия по роли + счёт
            ├── customer/
            │   ├── RoomsPage.tsx        # Поиск доступных номеров
            │   ├── BookingCreatePage.tsx # Создание брони + выбор услуг
            │   ├── MyBookingsPage.tsx
            │   ├── MenuPage.tsx         # Меню буфета + заказ (с выбором доставки)
            │   ├── MyOrdersPage.tsx     # История заказов клиента
            │   ├── ServicesPage.tsx     # Страница услуг с фото и описаниями
            │   └── InvoicesPage.tsx     # Счета + кнопка оплаты
            ├── reception/
            │   └── AllBookingsPage.tsx  # Все брони с фильтрами и действиями
            └── admin/
                ├── ManageRoomsPage.tsx
                ├── ManageMenuPage.tsx
                └── ManageAmenitiesPage.tsx  # Услуги: описание, фото, доступность
```

---

## 5. Быстрый старт (Docker Compose)

### Требования

- Docker Desktop ≥ 24
- Docker Compose v2 (встроен в Docker Desktop)
- Минимум 4 GB RAM для Docker

### Шаги

**1. Запустить весь стек:**

```bash
docker-compose up --build
```

> Первый запуск займёт 10–15 минут: Docker скачает базовые образы и соберёт JAR-файлы внутри контейнеров. Повторные запуски — значительно быстрее.

**2. Открыть приложение:**

```
http://localhost          → Приложение (фронтенд)
```

**3. Дополнительные адреса:**

```
http://localhost:8080      → API Gateway
http://localhost:3000      → Grafana (admin / admin)
http://localhost:3100      → Loki (API)
```

**4. Остановить:**

```bash
docker-compose down
# Чтобы удалить тома (БД, Grafana dashboards):
docker-compose down -v
```

### Порядок старта (автоматически через healthcheck depends_on)

```
PostgreSQL (ready) ──┐
                     ├──► Все микросервисы ──► API Gateway ──► Frontend
Kafka (ready) ───────┘
Redis (started) ─────┘
```

---

## 6. Демо-пользователи

После первого запуска `customer-service` автоматически создаёт трёх тестовых пользователей (если они ещё не существуют):

| Роль | Email | Пароль | Что доступно |
|---|---|---|---|
| CUSTOMER | `customer@hotel.com` | `customer123` | Поиск номеров, бронирование, буфет, счета |
| RECEPTION | `reception@hotel.com` | `reception123` | Все брони, подтверждение, заселение/выселение, оплата |
| ADMIN | `admin@hotel.com` | `admin123` | Всё + управление номерами, меню, услугами |

> Пользователи создаются только при отсутствии — повторные запуски безопасны. Данные сохраняются в PostgreSQL volume, поэтому пересборка образов их не удаляет. Для полного сброса: `docker-compose down -v`.

---

## 7. Локальная разработка

### Запуск только инфраструктуры в Docker

```bash
docker-compose up postgres redis kafka zookeeper -d
```

### Запуск backend-сервисов локально

Каждый сервис можно запустить отдельно. Пример для `customer-service`:

```bash
./gradlew :customer-service:bootRun
```

Или через IntelliJ IDEA: открой проект как Gradle project, запусти `*Application.java` каждого сервиса.

**Переменные окружения для локального запуска** (можно выставить в Run Configuration в IDEA):

```
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/hotel?currentSchema=customer
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=postgres
SPRING_KAFKA_BOOTSTRAP_SERVERS=localhost:9092
SPRING_REDIS_HOST=localhost
JWT_SECRET=very-secret-key-min-32-chars-long-here-ok
```

### Swagger UI каждого сервиса

После запуска сервиса Swagger доступен по адресу:

```
http://localhost:8081/swagger-ui.html  → customer-service
http://localhost:8082/swagger-ui.html  → room-service
http://localhost:8083/swagger-ui.html  → booking-service
http://localhost:8084/swagger-ui.html  → amenity-service
http://localhost:8085/swagger-ui.html  → dining-service
http://localhost:8086/swagger-ui.html  → billing-service
```

### Запуск frontend локально

```bash
cd frontend
npm install
npm run dev
```

Приложение запустится на `http://localhost:3001`.

> Vite проксирует все запросы `/api/*` на `http://localhost:8080` — API Gateway должен быть запущен.

---

## 8. Переменные окружения

### Общие для всех backend-сервисов

| Переменная | По умолчанию | Описание |
|---|---|---|
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5432/hotel?currentSchema=<schema>` | URL PostgreSQL |
| `SPRING_DATASOURCE_USERNAME` | `postgres` | Пользователь БД |
| `SPRING_DATASOURCE_PASSWORD` | `postgres` | Пароль БД |
| `SPRING_KAFKA_BOOTSTRAP_SERVERS` | `localhost:9092` | Адрес Kafka |
| `SPRING_REDIS_HOST` | `localhost` | Хост Redis |
| `SPRING_REDIS_PORT` | `6379` | Порт Redis |
| `JWT_SECRET` | `very-secret-key-min-32-chars-long-here-ok` | Секрет для подписи JWT |
| `JWT_EXPIRATION_MS` | `86400000` (24 часа) | Время жизни токена |

### Специфичные для сервисов

| Сервис | Переменная | Описание |
|---|---|---|
| booking-service | `ROOM_SERVICE_URL` | URL room-service (по умолчанию `http://room-service:8082`) |
| dining-service | `BOOKING_SERVICE_URL` | URL booking-service (по умолчанию `http://booking-service:8083`) |
| api-gateway | `CUSTOMER_SERVICE_URL` | `http://customer-service:8081` |
| api-gateway | `ROOM_SERVICE_URL` | `http://room-service:8082` |
| api-gateway | `BOOKING_SERVICE_URL` | `http://booking-service:8083` |
| api-gateway | `AMENITY_SERVICE_URL` | `http://amenity-service:8084` |
| api-gateway | `DINING_SERVICE_URL` | `http://dining-service:8085` |
| api-gateway | `BILLING_SERVICE_URL` | `http://billing-service:8086` |

---

## 9. API Reference

Все запросы идут через API Gateway на порт **8080**. Эндпоинты `/api/auth/register` и `/api/auth/login` публичные — остальные требуют `Authorization: Bearer <token>`.

### Аутентификация

| Метод | URL | Роль | Описание |
|---|---|---|---|
| `POST` | `/api/auth/register` | — | Регистрация нового клиента |
| `POST` | `/api/auth/login` | — | Вход, получение JWT |
| `GET` | `/api/customers/me` | любая | Профиль текущего пользователя |
| `GET` | `/api/customers/{id}` | RECEPTION, ADMIN | Профиль по ID |
| `GET` | `/api/customers` | ADMIN | Все клиенты |
| `PUT` | `/api/customers/{id}/role?role=RECEPTION` | ADMIN | Изменить роль |

**Пример регистрации:**
```json
POST /api/auth/register
{
  "email": "ivan@example.com",
  "password": "secret123",
  "firstName": "Иван",
  "lastName": "Иванов",
  "phone": "+7 999 123-45-67"
}
```

**Ответ:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "userId": 1,
  "email": "ivan@example.com",
  "role": "CUSTOMER"
}
```

### Номера

| Метод | URL | Роль | Описание |
|---|---|---|---|
| `GET` | `/api/rooms/search?checkIn=2025-06-01&checkOut=2025-06-05&guests=2` | любая | Поиск свободных номеров |
| `GET` | `/api/rooms` | ADMIN, RECEPTION | Все номера |
| `GET` | `/api/rooms/{id}` | любая | Номер по ID |
| `POST` | `/api/rooms` | ADMIN | Создать номер |
| `PUT` | `/api/rooms/{id}` | ADMIN | Обновить номер |
| `DELETE` | `/api/rooms/{id}` | ADMIN | Удалить номер |

**Пример создания номера:**
```json
POST /api/rooms
{
  "roomNumber": "101",
  "roomClass": "PREMIUM",
  "capacity": 2,
  "pricePerNight": 5000.00,
  "description": "Люкс с видом на море"
}
```

### Бронирование

| Метод | URL | Роль | Описание |
|---|---|---|---|
| `POST` | `/api/bookings` | CUSTOMER | Создать бронь |
| `GET` | `/api/bookings/my` | CUSTOMER | Мои брони |
| `GET` | `/api/bookings/all` | RECEPTION, ADMIN | Все брони |
| `GET` | `/api/bookings/{id}` | любая | Бронь по ID |
| `POST` | `/api/bookings/{id}/cancel` | CUSTOMER, RECEPTION | Отменить бронь |
| `POST` | `/api/bookings/{id}/confirm` | RECEPTION | Подтвердить бронь |
| `POST` | `/api/bookings/{id}/checkin` | RECEPTION | Заселить |
| `POST` | `/api/bookings/{id}/checkout` | RECEPTION | Выселить (→ формирует счёт) |

**Пример создания брони с услугами:**
```json
POST /api/bookings
{
  "roomId": 1,
  "checkIn": "2025-06-01",
  "checkOut": "2025-06-05",
  "amenities": [
    {
      "serviceType": "SAUNA",
      "startTime": "2025-06-02T18:00:00",
      "endTime": "2025-06-02T20:00:00"
    },
    {
      "serviceType": "MASSAGE",
      "startTime": "2025-06-03T10:00:00",
      "endTime": "2025-06-03T11:00:00"
    }
  ]
}
```

### Услуги (Amenities)

`GET /api/amenities/**` — **публичный маршрут**, токен не требуется (для лендинга и просмотра гостями).

| Метод | URL | Роль | Описание |
|---|---|---|---|
| `GET` | `/api/amenities` | — | Все услуги (публично) |
| `GET` | `/api/amenities/{id}` | — | Услуга по ID (публично) |
| `GET` | `/api/amenities/{id}/image` | — | Фото услуги (публично, `Content-Type: image/*`) |
| `GET` | `/api/amenities/type/{type}` | — | Услуги по типу (публично) |
| `POST` | `/api/amenities` | ADMIN | Создать услугу |
| `PUT` | `/api/amenities/{id}` | ADMIN | Обновить услугу (название, тип, цена, описание, доступность) |
| `DELETE` | `/api/amenities/{id}` | ADMIN | Удалить услугу |
| `POST` | `/api/amenities/{id}/image` | ADMIN | Загрузить фото (multipart/form-data, поле `file`, макс. 2 МБ) |

### Буфет

| Метод | URL | Роль | Описание |
|---|---|---|---|
| `GET` | `/api/menu` | любая | Меню буфета |
| `POST` | `/api/menu` | ADMIN | Добавить блюдо |
| `PUT` | `/api/menu/{id}` | ADMIN | Обновить блюдо |
| `DELETE` | `/api/menu/{id}` | ADMIN | Удалить блюдо |
| `POST` | `/api/orders` | CUSTOMER | Заказать еду |
| `GET` | `/api/orders/my` | CUSTOMER | Все мои заказы (сортировка: новые первые) |
| `GET` | `/api/orders/booking/{bookingId}` | CUSTOMER, RECEPTION | Заказы по брони |

**Пример заказа:**
```json
POST /api/orders
{
  "bookingId": 42,
  "menuItemId": 5,
  "quantity": 2,
  "deliveryType": "ROOM_DELIVERY"
}
```

### Счета (Billing)

| Метод | URL | Роль | Описание |
|---|---|---|---|
| `GET` | `/api/invoices/my` | CUSTOMER | Мои счета |
| `GET` | `/api/invoices/booking/{bookingId}` | любая | Счёт по брони |
| `POST` | `/api/invoices/{bookingId}/pay` | CUSTOMER, RECEPTION | Отметить как оплаченный |

---

## 10. Kafka события

Все сообщения сериализуются в JSON. Топики создаются автоматически при первом publish (`KAFKA_AUTO_CREATE_TOPICS_ENABLE=true`).

| Топик | Producer | Consumer(s) | Поля события |
|---|---|---|---|
| `booking.created` | booking-service | room-service, billing-service | bookingId, customerId, roomId, roomClass, checkIn, checkOut, totalPrice |
| `booking.confirmed` | booking-service | — | bookingId, customerId |
| `booking.cancelled` | booking-service | room-service | bookingId, customerId, roomId, penaltyAmount |
| `booking.completed` | booking-service | billing-service | bookingId, customerId, roomId, roomClass, checkIn, checkOut, roomTotal, amenitiesTotal |
| `order.created` | dining-service | billing-service | orderId, bookingId, customerId, totalAmount, paidByLimit, extraCharge, orderTime |
| `service.booked` | booking-service | amenity-service | bookingId, customerId, serviceType, startTime, endTime, price |
| `payment.processed` | billing-service | — | bookingId, invoiceId, totalAmount |

### Пример flow при выселении клиента

```
Ресепшн нажимает "Выселить"
        │
        ▼
booking-service.checkOut()
  → статус = COMPLETED
  → Kafka: booking.completed {roomTotal, amenitiesTotal}
        │
        ▼
billing-service (listener)
  → createInvoice() — создаёт счёт с UNPAID
  → (если ранее были заказы из буфета — diningAmount уже добавлен)
        │
        ▼
billing-service.pay() — ресепшн нажимает "Оплатить"
  → статус = PAID
  → Kafka: payment.processed
```

---

## 11. Redis кэширование

### room-service: кэш доступных номеров

- **Ключ**: `available-rooms::{checkIn}-{checkOut}-{guests}`
- **TTL**: 5 минут
- **Инвалидация**: при создании/изменении/удалении номера, при booking.created и booking.cancelled (через Kafka → `@CacheEvict(allEntries=true)`)

### dining-service: дневные лимиты буфета

- **Ключ**: `dining:limit:{bookingId}:{date}` (например `dining:limit:42:2025-06-03`)
- **TTL**: автоматически вычисляется до полуночи текущего дня по времени сервера
- **Логика**: при заказе еды читается текущая сумма расходов за день, сравнивается с лимитом класса номера, разница пишется в `extraCharge`
- **Сброс**: TTL истекает в полночь → следующий запрос начинает с 0

### Лимиты по классам:

| Класс | Лимит в день |
|---|---|
| ORDINARY | 0 руб. (всё платно) |
| MIDDLE | 1 000 руб. |
| PREMIUM | 3 000 руб. |

### dining-service: кэш меню

- **Ключ**: `menu-items`
- **TTL**: 1 час
- **Инвалидация**: при любом CRUD операции с меню

---

## 12. Безопасность и JWT

### Как работает авторизация

```
1. Клиент → POST /api/auth/login
2. customer-service: проверяет BCrypt пароль, генерирует JWT
   Payload: { "sub": "email", "userId": 1, "role": "CUSTOMER", "exp": ... }
3. Клиент сохраняет токен и передаёт его в заголовке:
   Authorization: Bearer eyJhbGc...
4. API Gateway (JwtAuthFilter):
   - Проверяет подпись токена
   - Если валидный: добавляет X-User-Id и X-User-Role к запросу
   - Если невалидный: возвращает 401
5. Downstream-сервисы:
   - НЕ проверяют JWT сами
   - Доверяют заголовкам X-User-Id и X-User-Role от Gateway
   - Используют @RequestHeader("X-User-Id") для получения ID пользователя
```

### Публичные маршруты (не требуют токен)

- `POST /api/auth/register`
- `POST /api/auth/login`
- `GET /swagger-ui/**`
- `GET /v3/api-docs/**`
- `GET /actuator/**`
- `GET /api/amenities/**` — только метод GET (POST/PUT/DELETE по-прежнему требуют ADMIN-токен)

### Важно для production

- Замените `JWT_SECRET` на сильный случайный ключ минимум 32 символа
- Уберите `ddl-auto: update` из application.yml, используйте Flyway/Liquibase
- Настройте HTTPS через nginx reverse proxy

---

## 13. Привилегии по классам номеров

### Дополнительные услуги

| Услуга | ORDINARY | MIDDLE | PREMIUM |
|---|---|---|---|
| Баня (2 ч) | 2 000 руб. | 1 400 руб. (−30%) | 1 шт. бесплатно, затем 2 000 руб. |
| Сауна (2 ч) | 2 000 руб. | 1 400 руб. (−30%) | 1 шт. бесплатно (общая квота с баней) |
| Бассейн (1 ч) | 500 руб. | 350 руб. (−30%) | Бесплатно |
| Бильярд (1 ч) | 600 руб. | 600 руб. | 600 руб. |
| Массаж | 3 000 руб. | 3 000 руб. | 1 шт. бесплатно, затем 3 000 руб. |

> Бесплатная квота Premium: одна SAUNA **или** одна BATH (что заказал первым — то и бесплатно). Если заказал обе — только первая бесплатна.

### Буфет

| Класс | Лимит/день | Поведение при превышении |
|---|---|---|
| ORDINARY | 0 руб. | Вся сумма — extraCharge, добавляется к счёту |
| MIDDLE | 1 000 руб. | До 1 000 — paidByLimit; свыше — extraCharge |
| PREMIUM | 3 000 руб. | До 3 000 — paidByLimit; свыше — extraCharge |

---

## 14. Логирование и мониторинг

### Формат логов (JSON)

Каждый сервис пишет структурированные JSON-логи в stdout через `logstash-logback-encoder`:

```json
{
  "@timestamp": "2025-06-01T10:00:00.000Z",
  "level": "INFO",
  "service": "booking-service",
  "logger_name": "com.pethotel.booking.service.BookingService",
  "thread_name": "http-nio-8083-exec-1",
  "message": "Booking created: id=42 customerId=1"
}
```

### Сбор логов

```
Docker container stdout
        │
        ▼
Promtail (scraping via Docker socket)
        │
        ▼
Loki :3100 (хранение)
        │
        ▼
Grafana :3000 (визуализация)
```

### Настройка Grafana

1. Открой `http://localhost:3000` (admin / admin)
2. **Add data source** → Loki → URL: `http://loki:3100`
3. **Explore** → выбери Loki → используй LogQL запросы:

```logql
# Все логи booking-service
{service="booking-service"}

# Только ошибки
{service="booking-service"} | json | level="ERROR"

# Логи по конкретному bookingId
{service="booking-service"} |= "bookingId=42"

# Все сервисы, только WARN и ERROR
{job=~".+"} | json | level=~"(WARN|ERROR)"
```

### Actuator Health

Каждый сервис предоставляет эндпоинты:

```
GET /actuator/health   → статус сервиса
GET /actuator/info     → информация о версии
GET /actuator/metrics  → метрики
```

---

## 15. Frontend

### Технологии

- **React 18** + **TypeScript 5** — типизированные компоненты
- **Vite 5** — dev сервер с HMR, быстрая сборка
- **TailwindCSS 3** — utility-first CSS
- **React Router DOM 6** — клиентский роутинг
- **Axios** — HTTP-клиент с interceptor для Bearer token и 401 redirect
- **Zustand** — минималистичный state management (auth state с persist в localStorage)

### Страницы по ролям

| Страница | URL | Роль |
|---|---|---|
| Главная (лендинг) | `/` | — (публичная) |
| Вход | `/login` | — |
| Регистрация | `/register` | — |
| Dashboard | `/dashboard` | все |
| Поиск номеров | `/rooms` | CUSTOMER, ADMIN |
| Услуги отеля | `/services` | CUSTOMER, RECEPTION, ADMIN |
| Создание брони | `/bookings/new?roomId=...` | CUSTOMER |
| Мои брони | `/bookings/my` | CUSTOMER |
| Меню буфета | `/menu` | CUSTOMER |
| Мои заказы | `/orders/my` | CUSTOMER |
| Мои счета | `/invoices` | CUSTOMER |
| Детали брони | `/bookings/:id` | все |
| Все брони | `/bookings/all` | RECEPTION, ADMIN |
| Управление номерами | `/rooms/manage` | ADMIN |
| Управление меню | `/menu/manage` | ADMIN |
| Управление услугами | `/amenities/manage` | ADMIN |

### Структура API-слоя

```
src/api/client.ts        ← Axios instance: baseURL=/api, Bearer token, 401→/login
src/api/auth.api.ts      ← authApi.register(), authApi.login()
src/api/room.api.ts      ← roomApi.search(), roomApi.getAll(), CRUD
src/api/booking.api.ts   ← bookingApi.create(), getMyBookings(), confirm(), ...
src/api/amenity.api.ts   ← amenityApi.getAll(), CRUD
src/api/dining.api.ts    ← diningApi.getMenu(), createOrder(), ...
src/api/billing.api.ts   ← billingApi.getMyInvoices(), pay()
```

### Auth store (Zustand)

```typescript
const { isAuthenticated, role, userId, login, logout } = useAuthStore()
```

Токен хранится в `localStorage`, при перезагрузке страницы стор инициализируется из него.

### Запуск frontend в Docker

Frontend собирается в nginx-образ. Все `/api/*` запросы nginx проксирует на `api-gateway:8080`.

---

## 16. Роли и права доступа

### Создание пользователей с нужной ролью

По умолчанию все зарегистрированные пользователи получают роль `CUSTOMER`. Чтобы назначить роль RECEPTION или ADMIN:

```bash
# 1. Зарегистрируй пользователя
POST /api/auth/register { "email": "admin@hotel.com", "password": "admin123", ... }

# 2. Войди как другой ADMIN
POST /api/auth/login { "email": "...", "password": "..." }

# 3. Измени роль
PUT /api/customers/{id}/role?role=ADMIN
Authorization: Bearer <admin-token>
```

> На начальном этапе разработки нет bootstrap-пользователя. Для первой настройки можно временно закомментировать проверку роли в CustomerController или добавить `INSERT INTO customer.customers ...` в `init-db.sql`.

### Матрица прав

| Действие | CUSTOMER | RECEPTION | ADMIN |
|---|---|---|---|
| Регистрация/Вход | ✅ | ✅ | ✅ |
| Поиск номеров | ✅ | ✅ | ✅ |
| Создание брони | ✅ | — | — |
| Просмотр своих броней | ✅ | — | — |
| Отмена своей брони | ✅ | — | — |
| Просмотр всех броней | — | ✅ | ✅ |
| Подтверждение/Заселение/Выселение | — | ✅ | ✅ |
| Заказ еды | ✅ | — | — |
| Просмотр своих заказов | ✅ | — | — |
| Просмотр своих счетов | ✅ | — | — |
| Оплата счёта | ✅ | ✅ | ✅ |
| CRUD номеров | — | — | ✅ |
| CRUD меню | — | — | ✅ |
| CRUD услуг | — | — | ✅ |
| Управление ролями | — | — | ✅ |

---

## 17. Устранение проблем

### Сервис не стартует — ошибка подключения к БД

```
Failed to obtain JDBC Connection
```

**Причина**: PostgreSQL ещё не готов.  
**Решение**: `docker-compose up` повторит запуск сервиса автоматически через `restart: on-failure`. Подожди 30–60 секунд.

---

### Kafka: `LEADER_NOT_AVAILABLE`

**Причина**: Kafka ещё инициализируется.  
**Решение**: Сервисы с Kafka-продюсерами переподключатся автоматически. Это нормально при первом запуске.

---

### Frontend показывает 401 на все запросы

**Причина**: Неверный или истёкший JWT токен.  
**Решение**: Выйди и войди снова. Проверь, что `JWT_SECRET` одинаковый в `customer-service` и `api-gateway`.

---

### Ошибка при сборке Gradle: `Could not resolve`

```bash
./gradlew build --refresh-dependencies
```

---

### Порт уже занят

```bash
# Найти процесс на порту 8080
netstat -ano | findstr :8080  # Windows
lsof -i :8080                 # macOS/Linux
```

---

### Посмотреть логи конкретного сервиса

```bash
docker-compose logs -f booking-service
docker-compose logs -f api-gateway
```

---

### Пересобрать только один сервис

```bash
docker-compose up --build booking-service
```

---

### Проверить состояние Kafka топиков

```bash
docker-compose exec kafka kafka-topics --list --bootstrap-server localhost:9092
docker-compose exec kafka kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic booking.created \
  --from-beginning
```

---

### Очистить Redis

```bash
docker-compose exec redis redis-cli FLUSHALL
```

---

## Лицензия

MIT — Pet project для изучения микросервисной архитектуры.
