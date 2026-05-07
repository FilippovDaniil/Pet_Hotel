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
| `support-service` | 8087 | `support` | Чат поддержки клиент↔️admin |
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
  - `X-User-Email: <String>`
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
CREATE SCHEMA IF NOT EXISTS customer, room, booking, amenity, dining, billing, support;
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
| `/services` | CUSTOMER, RECEPTION, ADMIN | `pages/customer/ServicesPage.tsx` |
| `/bookings/new` | CUSTOMER | `pages/customer/BookingCreatePage.tsx` |
| `/bookings/my` | CUSTOMER | `pages/customer/MyBookingsPage.tsx` |
| `/menu` | CUSTOMER | `pages/customer/MenuPage.tsx` |
| `/invoices` | CUSTOMER | `pages/customer/InvoicesPage.tsx` |
| `/bookings/:id` | все | `pages/BookingDetailPage.tsx` |
| `/bookings/all` | RECEPTION, ADMIN | `pages/reception/AllBookingsPage.tsx` |
| `/rooms/manage` | ADMIN | `pages/admin/ManageRoomsPage.tsx` |
| `/menu/manage` | ADMIN | `pages/admin/ManageMenuPage.tsx` |
| `/amenities/manage` | ADMIN | `pages/admin/ManageAmenitiesPage.tsx` |
| `/support` | CUSTOMER | `pages/customer/SupportPage.tsx` |
| `/support/admin` | ADMIN | `pages/admin/AdminSupportPage.tsx` |

---

## Сборка и запуск

```bash
# Запустить всё (JAR собирается внутри Docker, pre-build не нужен)
docker-compose up --build

# Только инфраструктура (для локальной разработки)
docker-compose up postgres redis kafka zookeeper -d

# Один сервис
./gradlew :booking-service:bootRun

# Frontend dev
cd frontend && npm run dev
```

## Docker (multi-stage build)

Каждый backend Dockerfile — **multi-stage build** с BuildKit cache:

```dockerfile
# syntax=docker/dockerfile:1
FROM gradle:jdk17-alpine AS builder   # содержит JDK + Gradle CLI
WORKDIR /workspace
COPY . .
RUN --mount=type=cache,target=/root/.gradle \
    gradle :<service>:build -x test --no-daemon
    # --mount=type=cache: ~/.gradle кэшируется между сборками
    # -x test: тесты не запускаются в Docker
    # --no-daemon: daemon не нужен в одноразовом контейнере

FROM eclipse-temurin:17-jre-alpine   # только JRE (~100 МБ вместо ~300 МБ)
WORKDIR /app
COPY --from=builder /workspace/<service>/build/libs/*.jar app.jar
EXPOSE <port>
ENTRYPOINT ["java", "-jar", "app.jar"]  # exec form: PID 1 = java → корректный SIGTERM
```

**В docker-compose:** `context: .` для всех backend-сервисов (Gradle видит весь multi-project включая `common`).
Frontend: `context: ./frontend` (отдельный context: node build → nginx).

## CI/CD

Файлы в `.github/`:

| Файл | Триггер | Что делает |
|---|---|---|
| `workflows/ci.yml` | push/PR к master | Сборка + тесты backend, lint + build frontend |
| `workflows/docker-publish.yml` | push тега `v*` | Публикует Docker образы в GHCR |
| `workflows/codeql.yml` | push/PR/еженедельно | Статический анализ (Java + TS) |
| `dependabot.yml` | еженедельно | Обновление зависимостей gradle/npm/actions |

**Образы:** `ghcr.io/filippovdaniil/pet-hotel-<service>:latest`

---

## Логирование

Все сервисы используют `logstash-logback-encoder` → JSON в stdout → Promtail → Loki → Grafana.

- Grafana: `http://localhost:3000` (admin/admin)
- Loki datasource — **автоматически** через `grafana/provisioning/datasources/loki.yml`
- Дашборд **Pet Hotel — Logs** — автоматически через `grafana/provisioning/dashboards/pet-hotel.json`
- Поле `service` в каждом логе (задано в `logback-spring.xml` через `<customFields>`)
- Все logback используют `<fieldNames><timestamp>timestamp</timestamp>...` — поле `timestamp` (не `@timestamp`)
- Promtail собирает логи через Docker socket (`/var/run/docker.sock`), извлекает `level` и `service` как stream labels

**Структура provisioning:**
```
grafana/provisioning/
  datasources/loki.yml       → Loki datasource (uid: loki, url: http://loki:3100)
  dashboards/dashboards.yml  → provider config (folder: Pet Hotel)
  dashboards/pet-hotel.json  → дашборд: stats, timeseries по уровням/сервисам, лог-панели
```

**LogQL примеры:**
```
{service="booking-service"} | json                     # логи сервиса
{service=~"booking-service|billing-service"} | json    # несколько сервисов
{level="ERROR"} | json                                 # только ошибки
{service=~".*", level="ERROR"} | json                  # ошибки по всем сервисам
```

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
SUPPORT_SERVICE_URL      http://support-service:8087
```

---

## Тесты

### Запуск

```bash
# Unit-тесты (без инфраструктуры, файлы *Test.java)
./gradlew test

# Интеграционные тесты (требуют запущенный Docker Desktop, файлы *IT.java)
./gradlew integrationTest

# Конкретный сервис
./gradlew :customer-service:integrationTest
```

### Структура

| Тип | Аннотация | Инфраструктура | Где |
|---|---|---|---|
| Unit | `@ExtendWith(MockitoExtension.class)` | нет | все сервисы: `*Test.java` |
| Web slice | `@WebMvcTest` + `@MockBean` | нет | `AuthControllerTest` |
| Integration (HTTP) | `@SpringBootTest(RANDOM_PORT)` + `@Testcontainers` | PostgreSQL (Docker) | `*IT.java` |
| Integration (JPA) | `@DataJpaTest` + `@Testcontainers` | PostgreSQL (Docker) | `RoomRepositoryIT` |

### Ключевые паттерны unit-тестов

- **BigDecimal:** `assertThat(...).isEqualByComparingTo("1000")` — не `isEqualTo` (избегает scale-проблем)
- **Kafka events:** `ArgumentCaptor<Object>` + cast; `verify(kafkaTemplate).send(eq(topic), any())`
- **`@WebMvcTest`** требует `@Import(GlobalExceptionHandler.class)` для корректных HTTP-статусов
- **`@Value`-поля** не инжектируются через `@InjectMocks`; если нужны в тесте — `ReflectionTestUtils.setField`
- **WebClient mock-цепочка:** `Builder → WebClient → RequestHeadersUriSpec → RequestHeadersSpec → ResponseSpec`
- **Mockito + generics:** `@SuppressWarnings("unchecked")` на классе при работе с `WebClient.*Spec`
- **Premium SAUNA/BATH quota:** считается по количеству записей с `price == 0` в `booking.amenities`, SAUNA и BATH делят одну квоту

### Ключевые паттерны интеграционных тестов

- **`@ServiceConnection`** на `PostgreSQLContainer` автоматически конфигурирует datasource (Spring Boot 3.1+)
- **Init script:** `new PostgreSQLContainer<>(...).withInitScript("create-<service>-schema.sql")` — создаёт PostgreSQL-схему ДО того, как Hibernate делает DDL
- **EmbeddedKafka:** `@EmbeddedKafka(partitions = 1)` для сервисов, которые реально пишут в Kafka (booking, billing)
- **Kafka для customer-service:** `spring.kafka` есть в зависимостях, но CustomerService не использует KafkaTemplate — контекст стартует без запущенного Kafka (lazy connection)

## Жизненный цикл бронирования

```
PENDING → CONFIRMED (confirm)
PENDING/CONFIRMED → CANCELLED (cancel)
CONFIRMED → COMPLETED (checkOut → через checkIn)
```

**Допустимые переходы:** `confirm` только из PENDING; `cancel` только из PENDING/CONFIRMED; `checkIn` только из CONFIRMED; `checkOut` только из CONFIRMED (устанавливает COMPLETED).

**Штраф при отмене клиентом:** 30% от `totalPrice` если `LocalDate.now().plusDays(1).isAfter(checkInDate)` (то есть заезд сегодня или завтра = в пределах 24 ч). Штраф не применяется если `isReception = true`.

**Владелец:** `cancel` разрешён только если `requesterId == booking.customerId` OR `isReception = true`.

## Тестовые данные (DataSeeder)

При первом старте каждый сервис автоматически заполняет БД, если она пуста:

| Сервис | Файл | Данные |
|---|---|---|
| customer-service | `config/DataSeeder.java` | 3 пользователя: customer, reception, admin |
| room-service | `config/DataSeeder.java` | 10 номеров: 4×ORDINARY, 3×MIDDLE, 3×PREMIUM |
| amenity-service | `config/DataSeeder.java` | 6 услуг: сауна, баня, бассейн, 2×бильярд, массаж |
| dining-service | `config/DataSeeder.java` | 26 позиций меню: завтраки, обед, ужин, напитки, десерты |

Все сидеры идемпотентны (`count() == 0` перед вставкой). Данные сохраняются в volume PostgreSQL — `docker-compose down` без `-v` их не удаляет.

### Тестовые учётные данные

| Email | Пароль | Роль |
|---|---|---|
| `customer@hotel.com` | `customer123` | CUSTOMER |
| `reception@hotel.com` | `reception123` | RECEPTION |
| `admin@hotel.com` | `admin123` | ADMIN |

---

## REST API — быстрая справка

### customer-service (8081)

| Метод | Путь | Описание |
|---|---|---|
| POST | `/api/auth/register` | Регистрация; возвращает `{token, userId, email, role}` |
| POST | `/api/auth/login` | Логин; возвращает `{token, userId, email, role}` |

### room-service (8082)

| Метод | Путь | Роль | Описание |
|---|---|---|---|
| GET | `/api/rooms` | все | Все номера |
| GET | `/api/rooms/available` | все | Свободные (`?checkIn=&checkOut=&guests=`) |
| GET | `/api/rooms/{id}` | все | Один номер |
| POST | `/api/rooms` | ADMIN | Создать номер |
| PUT | `/api/rooms/{id}` | ADMIN | Обновить номер |
| DELETE | `/api/rooms/{id}` | ADMIN | Удалить номер |
| GET | `/api/rooms/{id}/image` | все | Изображение номера (`image/*`) |
| POST | `/api/rooms/{id}/image` | ADMIN | Загрузить изображение (multipart) |

### booking-service (8083)

| Метод | Путь | Роль | Описание |
|---|---|---|---|
| POST | `/api/bookings` | CUSTOMER | Создать бронирование + услуги |
| GET | `/api/bookings/my` | CUSTOMER | Мои бронирования |
| GET | `/api/bookings/all` | RECEPTION/ADMIN | Все бронирования |
| GET | `/api/bookings/{id}` | все | Детали бронирования |
| POST | `/api/bookings/{id}/confirm` | RECEPTION | PENDING → CONFIRMED |
| POST | `/api/bookings/{id}/cancel` | CUSTOMER/RECEPTION | → CANCELLED (штраф если < 24 ч) |
| POST | `/api/bookings/{id}/check-in` | RECEPTION | Фиксирует заезд |
| POST | `/api/bookings/{id}/check-out` | RECEPTION | CONFIRMED → COMPLETED |
| POST | `/api/bookings/{id}/amenities` | CUSTOMER | Добавить услугу к бронированию |

### amenity-service (8084)

| Метод | Путь | Роль | Описание |
|---|---|---|---|
| GET | `/api/amenities` | публично (GET) | Все услуги |
| POST | `/api/amenities` | ADMIN | Создать услугу |
| PUT | `/api/amenities/{id}` | ADMIN | Обновить услугу |
| DELETE | `/api/amenities/{id}` | ADMIN | Удалить услугу |
| GET | `/api/amenities/{id}/image` | все | Изображение услуги |
| POST | `/api/amenities/{id}/image` | ADMIN | Загрузить изображение (multipart, max 2 МБ) |

### dining-service (8085)

| Метод | Путь | Роль | Описание |
|---|---|---|---|
| GET | `/api/menu` | все | Все позиции меню |
| POST | `/api/menu` | ADMIN | Добавить позицию |
| PUT | `/api/menu/{id}` | ADMIN | Обновить позицию |
| DELETE | `/api/menu/{id}` | ADMIN | Удалить позицию |
| POST | `/api/orders` | CUSTOMER | Создать заказ (тело: `bookingId`, `menuItemId`, `quantity`, `deliveryType`) |
| GET | `/api/orders/booking/{bookingId}` | все | Заказы по бронированию |

### billing-service (8086)

| Метод | Путь | Роль | Описание |
|---|---|---|---|
| GET | `/api/invoices/my` | CUSTOMER | Мои счета (по X-User-Id) |
| GET | `/api/invoices/booking/{bookingId}` | все | Счёт по бронированию |
| POST | `/api/invoices/{bookingId}/pay` | RECEPTION | Оплатить счёт → PAID |

---

## support-service API

| Метод | Путь | Роль | Описание |
|---|---|---|---|
| GET | `/api/support/messages` | CUSTOMER | Вся переписка клиента |
| POST | `/api/support/messages` | CUSTOMER | Отправить сообщение |
| GET | `/api/support/messages/unread-count` | CUSTOMER | Кол-во непрочитанных ответов от admin |
| POST | `/api/support/messages/read` | CUSTOMER | Отметить ответы admin как прочитанные |
| GET | `/api/support/admin/conversations` | ADMIN | Список всех диалогов с summary |
| GET | `/api/support/admin/conversations/{id}` | ADMIN | Все сообщения с конкретным клиентом |
| POST | `/api/support/admin/conversations/{id}/messages` | ADMIN | Ответить клиенту |
| POST | `/api/support/admin/conversations/{id}/read` | ADMIN | Отметить сообщения клиента как прочитанные |

**Сущность `SupportMessage`** (`support.messages`): `id`, `customerId`, `customerEmail`, `senderRole` (CUSTOMER/ADMIN), `content`, `createdAt`, `readByCustomer`, `readByAdmin`.

---

## Жизненный цикл счёта (billing-service)

Счёт формируется тремя независимыми Kafka-событиями:

```
booking.created   → initInvoice()      черновик: roomAmount = totalPrice (предварительно)
                                        amenitiesAmount = 0, diningAmount = 0
booking.completed → createInvoice()    финализация: точные roomAmount + amenitiesAmount
                                        diningAmount сохраняется (накоплен отдельно)
order.created     → addDiningCharge()  +extraCharge к diningAmount (только если > 0)
                                        totalAmount пересчитывается из трёх составляющих
HTTP POST /pay    → pay()              UNPAID → PAID; публикует payment.processed в Kafka
```

**Идемпотентность:** `initInvoice` проверяет `findByBookingId().isPresent()` — повторные Kafka-сообщения игнорируются. `createInvoice` обновляет существующий счёт или создаёт новый (страховка от out-of-order событий).

---

## Ключевые паттерны реализации

Нетривиальные детали, которые важно знать при правке кода:

### BookingService — двойной save()
```java
booking = bookingRepository.save(booking);  // 1-й: получить id для BookingAmenity.booking (FK)
// ... создаём BookingAmenity с booking.id ...
booking.setTotalPrice(roomTotal.add(amenitiesTotal));
booking = bookingRepository.save(booking);  // 2-й: сохранить услуги через cascade + totalPrice
```

### AmenityPriceCalculator — определение PREMIUM-квоты
```java
// SAUNA и BATH делят одну бесплатную квоту.
// Квота свободна если ни одного элемента с price==0 среди SAUNA/BATH в booking.amenities
boolean quotaFree = booking.getAmenities().stream()
    .noneMatch(a -> (a.getServiceType() == SAUNA || a.getServiceType() == BATH)
                 && a.getPrice().compareTo(BigDecimal.ZERO) == 0);
```

### BookingAmenityRepository — перекрытие временных слотов
```java
// Классический предикат пересечения интервалов: A.start < B.end AND B.start < A.end
// Работает для всех видов пересечений (частичное, полное включение)
WHERE ba.startTime < :endTime AND ba.endTime > :startTime
```

### DailyLimitService — TTL до полуночи
```java
// Ключ: "dining:limit:{bookingId}:{date}" — сбрасывается автоматически в полночь
Duration ttl = Duration.between(LocalDateTime.now(),
    date.plusDays(1).atTime(LocalTime.MIDNIGHT));
// toPlainString(): "1250.00", не "1.25E+3" — важно для StringRedisTemplate
stringRedisTemplate.opsForValue().set(key, amount.toPlainString(), ttl);
```

### OrderService — расщепление paidByLimit / extraCharge
```java
// Инвариант: paidByLimit + extraCharge == totalAmount
if (dailyLimit == 0 || remaining <= 0) {
    paidByLimit = ZERO; extraCharge = totalAmount;         // ORDINARY или лимит исчерпан
} else if (remaining >= totalAmount) {
    paidByLimit = totalAmount; extraCharge = ZERO;         // укладывается в лимит
} else {
    paidByLimit = remaining; extraCharge = totalAmount.subtract(remaining);  // частично
}
```

### fetchRoomClass — fallback на ORDINARY
```java
// При ошибке WebClient (booking-service недоступен) → ORDINARY.
// Безопасная сторона: не даём бесплатную еду по ошибке (ORDINARY = лимит 0 руб.)
private RoomClass fetchRoomClass(Long bookingId) {
    try { ... return response.getRoomClass(); }
    catch (Exception e) { return RoomClass.ORDINARY; }
}
```

### JwtAuthFilter — публичные пути (api-gateway)
```java
// anyRequest().startsWith() — /swagger-ui/index.html тоже проходит
List<String> PUBLIC_PATHS = List.of("/api/auth/register", "/api/auth/login",
    "/swagger-ui", "/v3/api-docs", "/actuator");
// GET /api/amenities публично (просмотр каталога без логина)
List<String> PUBLIC_GET_PATHS = List.of("/api/amenities");
// Приоритет фильтра: getOrder() = -100 (выполняется первым из всех GlobalFilter)
```

### Redis в dining-service — два механизма, один Redis
```java
// 1. RedisCacheManager (@Cacheable "menu-items") — Spring Cache, TTL 1 час
//    Ключи: "menu-items::<SpEL>" — управляется через @CacheEvict(allEntries=true)
// 2. StringRedisTemplate (DailyLimitService) — прямой доступ, ключи "dining:limit:*"
//    Пространства имён не пересекаются → конфликтов нет
```

### RoomUnavailableDate — денормализованная структура дат
```java
// Одна строка = один день (не диапазон).
// Блокировка: INSERT N строк для [checkIn, checkOut).
// Поиск: NOT IN (даты из диапазона) — проще SQL, чем проверка пересечения периодов.
// Разблокировка при отмене: удаляем только от now() вперёд (прошедшие даты неважны).
```

---

## Что ещё НЕ сделано (потенциальные задачи)

- Flyway/Liquibase вместо `ddl-auto: update`
- Bootstrap admin-пользователь (сейчас нет способа стать ADMIN без ручного SQL)
- Swagger через Gateway (сейчас только напрямую к каждому сервису)
- Трассировка запросов (traceId корреляция между сервисами)
- docker-compose.override.yml для dev (hot reload)
- WebSocket для real-time чата поддержки (сейчас polling каждые 15 с)
- Уведомления о новых сообщениях (бейдж в navbar)
