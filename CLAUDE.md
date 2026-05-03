# Pet Hotel — CLAUDE.md

Справочник для Claude Code. Читается автоматически в начале каждой сессии.

---

## Что это за проект

Микросервисный backend + frontend для управления отелем. Gradle multi-project, Java 17, Spring Boot 3.3.5. Pet-проект для изучения микросервисной архитектуры.

**Роли:** CUSTOMER (бронирование, буфет, счета), RECEPTION (заселение/выселение), ADMIN (управление данными).

---

## Модули и порты

| Модуль | Порт | Схема БД | Назначение |
|---|---|---|---|
| `common` | — | — | Shared: enums, Kafka events, JwtUtil |
| `customer-service` | 8081 | `customer` | Регистрация, логин, JWT |
| `room-service` | 8082 | `room` | CRUD номеров, проверка доступности |
| `booking-service` | 8083 | `booking` | Бронирование + услуги |
| `amenity-service` | 8084 | `amenity` | Управление услугами |
| `dining-service` | 8085 | `dining` | Буфет, лимиты, заказы |
| `billing-service` | 8086 | `billing` | Счета, оплата |
| `api-gateway` | 8080 | — | JWT-фильтр, маршрутизация |
| `frontend` | 80 (prod) / 3001 (dev) | — | React + Vite + Tailwind |

---

## Структура каждого backend-сервиса

```
<service>/
├── build.gradle
├── Dockerfile
└── src/main/
    ├── java/com/pethotel/<service>/
    │   ├── <Service>Application.java
    │   ├── entity/
    │   ├── repository/
    │   ├── dto/
    │   ├── service/
    │   ├── controller/
    │   └── config/
    │       ├── SecurityConfig.java      # stateless, все permitAll — gateway делает auth
    │       └── GlobalExceptionHandler.java
    └── resources/
        ├── application.yml
        └── logback-spring.xml           # LogstashEncoder, JSON логи
```

**Пакет:** `com.pethotel.<servicename>` (например `com.pethotel.booking`).

---

## Общая библиотека (common)

**Пакет:** `com.pethotel.common`

```
enums/
  Role.java            → CUSTOMER, RECEPTION, ADMIN
  RoomClass.java       → ORDINARY, MIDDLE, PREMIUM
  BookingStatus.java   → PENDING, CONFIRMED, CANCELLED, COMPLETED
  ServiceType.java     → SAUNA, BATH, POOL, BILLIARD_RUS, BILLIARD_US, MASSAGE
event/
  BookingCreatedEvent, BookingConfirmedEvent, BookingCancelledEvent,
  BookingCompletedEvent, ServiceBookedEvent, OrderCreatedEvent
kafka/
  KafkaTopics.java     → константы топиков
security/
  JwtUtil.java         → new JwtUtil(secret, expirationMs)
                         методы: generateToken(), parseToken(), isValid(), getUserId(), getRole(), getEmail()
```

---

## Безопасность (JWT)

- JWT выдаёт только `customer-service` при логине/регистрации
- `api-gateway` (`JwtAuthFilter.java`) валидирует токен и добавляет заголовки:
  - `X-User-Id: <Long>`
  - `X-User-Role: <String>`
- Downstream-сервисы **не проверяют JWT**, доверяют этим заголовкам
- Все `SecurityConfig` в downstream: `anyRequest().permitAll()`
- Публичные пути в gateway: `/api/auth/**`, `/swagger-ui/**`, `/v3/api-docs/**`, `/actuator/**`
- Env: `JWT_SECRET=very-secret-key-min-32-chars-long-here-ok`

---

## Kafka топики

| Топик | Producer | Consumer |
|---|---|---|
| `booking.created` | booking-service | room-service (блокирует даты), billing-service |
| `booking.confirmed` | booking-service | — |
| `booking.cancelled` | booking-service | room-service (разблокирует даты) |
| `booking.completed` | booking-service | billing-service (создаёт счёт) |
| `order.created` | dining-service | billing-service (добавляет к счёту) |
| `service.booked` | booking-service | amenity-service |
| `payment.processed` | billing-service | — |

**Kafka config:** `spring.json.add.type.headers: false` (producer), `spring.json.trusted.packages: "com.pethotel.common.event"` (consumer).

---

## Redis

| Сервис | Ключ | TTL | Назначение |
|---|---|---|---|
| room-service | `available-rooms::{checkIn}-{checkOut}-{guests}` | 5 минут | Кэш свободных номеров |
| room-service | `room-prices` | 1 час | Цены (резерв) |
| dining-service | `dining:limit:{bookingId}:{date}` | До полуночи | Дневные расходы буфета |
| dining-service | `menu-items` | 1 час | Кэш меню |

---

## Межсервисное HTTP (WebClient)

- `booking-service → room-service`: `GET /api/rooms/{id}` — получить класс номера для расчёта привилегий
  - Клиент: `booking-service/.../client/RoomClient.java`
  - URL из env: `ROOM_SERVICE_URL=http://room-service:8082`
- `dining-service → booking-service`: `GET /api/bookings/{id}` — получить roomClass для лимита буфета
  - URL из env: `BOOKING_SERVICE_URL=http://booking-service:8083`

---

## Привилегии по классам номеров

### Дополнительные услуги (AmenityPriceCalculator.java в booking-service)

| ServiceType | ORDINARY | MIDDLE | PREMIUM |
|---|---|---|---|
| SAUNA | 2 000 | 1 400 (×0.70) | 0 (первый), затем 2 000 |
| BATH | 2 000 | 1 400 (×0.70) | 0 (первый, квота с SAUNA), затем 2 000 |
| POOL | 500 | 350 (×0.70) | 0 (всегда бесплатно) |
| BILLIARD_RUS/US | 600 | 600 | 600 |
| MASSAGE | 3 000 | 3 000 | 0 (первый), затем 3 000 |

> Premium: SAUNA и BATH делят одну бесплатную квоту — первый заказ любого из двух = 0 руб.

### Буфет (DailyLimitService.java в dining-service)

| Класс | Лимит/день |
|---|---|
| ORDINARY | 0 руб. |
| MIDDLE | 1 000 руб. |
| PREMIUM | 3 000 руб. |

Расход хранится в Redis с TTL до полуночи. Превышение → `extraCharge` добавляется к счёту через Kafka `order.created`.

---

## База данных

Один PostgreSQL (`hotel`), разные схемы. `ddl-auto: update` — Hibernate создаёт таблицы автоматически.

**Схемы создаются из `init-db.sql`** (монтируется в `/docker-entrypoint-initdb.d/`):
```sql
CREATE SCHEMA IF NOT EXISTS customer, room, booking, amenity, dining, billing;
```

**Entities используют `@Table(schema="<name>")`** и `hibernate.default_schema` в application.yml.

---

## Frontend

**Расположение:** `frontend/`  
**Dev:** `cd frontend && npm install && npm run dev` → `http://localhost:3001`  
**Proxy:** Vite проксирует `/api/*` → `http://localhost:8080`  
**Prod:** nginx на порту 80, проксирует `/api/*` → `api-gateway:8080`

### Слои

```
src/types/       → TypeScript интерфейсы (Room, Booking, Invoice...)
src/api/         → Axios-клиенты (один файл на сервис)
src/api/client.ts → Axios instance: Bearer token interceptor + 401→/login redirect
src/store/auth.store.ts → Zustand: { token, userId, role, isAuthenticated, login(), logout() }
src/components/layout/Layout.tsx → Navbar с роль-зависимыми ссылками + <Outlet />
src/App.tsx      → Router + RequireAuth + RequireRole guards
src/pages/       → Страницы по ролям
```

### CSS-классы (определены в index.css)

`.btn-primary`, `.btn-secondary`, `.btn-danger`, `.btn-success`, `.input`, `.label`, `.card`, `.page-title`, `.section-title`

### Страницы

| URL | Роль | Компонент |
|---|---|---|
| `/login`, `/register` | публичные | `pages/auth/` |
| `/dashboard` | все | `pages/DashboardPage.tsx` |
| `/rooms` | CUSTOMER, ADMIN | `pages/customer/RoomsPage.tsx` |
| `/bookings/new` | CUSTOMER | `pages/customer/BookingCreatePage.tsx` |
| `/bookings/my` | CUSTOMER | `pages/customer/MyBookingsPage.tsx` |
| `/menu` | CUSTOMER | `pages/customer/MenuPage.tsx` |
| `/invoices` | CUSTOMER | `pages/customer/InvoicesPage.tsx` |
| `/bookings/:id` | все | `pages/BookingDetailPage.tsx` |
| `/bookings/all` | RECEPTION, ADMIN | `pages/reception/AllBookingsPage.tsx` |
| `/rooms/manage` | ADMIN | `pages/admin/ManageRoomsPage.tsx` |
| `/menu/manage` | ADMIN | `pages/admin/ManageMenuPage.tsx` |
| `/amenities/manage` | ADMIN | `pages/admin/ManageAmenitiesPage.tsx` |

---

## Сборка и запуск

```bash
# Собрать все JAR
./gradlew build -x test          # Unix
gradlew.bat build -x test        # Windows

# Запустить всё в Docker
docker-compose up --build

# Только инфраструктура (для локальной разработки)
docker-compose up postgres redis kafka zookeeper -d

# Один сервис
./gradlew :booking-service:bootRun

# Frontend dev
cd frontend && npm run dev
```

---

## Логирование

Все сервисы используют `logstash-logback-encoder` → JSON в stdout → Promtail → Loki → Grafana.

- Grafana: `http://localhost:3000` (admin/admin)
- Loki datasource URL: `http://loki:3100`
- Поле `service` в каждом логе (задано в `logback-spring.xml` через `<customFields>`)

---

## Gradle version catalog

Файл: `gradle/libs.versions.toml`

Ключевые алиасы плагинов: `libs.plugins.spring.boot`, `libs.plugins.spring.dependency.management`  
Spring Cloud BOM подключается вручную в каждом `build.gradle`:
```groovy
dependencyManagement {
    imports {
        mavenBom "org.springframework.cloud:spring-cloud-dependencies:${libs.versions.spring.cloud.get()}"
    }
}
```

---

## Переменные окружения (defaults)

```
SPRING_DATASOURCE_URL    jdbc:postgresql://localhost:5432/hotel?currentSchema=<schema>
SPRING_DATASOURCE_USERNAME  postgres
SPRING_DATASOURCE_PASSWORD  postgres
SPRING_KAFKA_BOOTSTRAP_SERVERS  localhost:9092
SPRING_REDIS_HOST        localhost
JWT_SECRET               very-secret-key-min-32-chars-long-here-ok
JWT_EXPIRATION_MS        86400000
ROOM_SERVICE_URL         http://room-service:8082
BOOKING_SERVICE_URL      http://booking-service:8083
```

---

## Что ещё НЕ сделано (потенциальные задачи)

- Тесты (Testcontainers для интеграционных)
- Flyway/Liquibase вместо `ddl-auto: update`
- Bootstrap admin-пользователь (сейчас нет способа стать ADMIN без ручного SQL)
- Swagger через Gateway (сейчас только напрямую к каждому сервису)
- Трассировка запросов (traceId корреляция между сервисами)
- docker-compose.override.yml для dev (hot reload)
