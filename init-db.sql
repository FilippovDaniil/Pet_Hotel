-- Скрипт инициализации БД для Pet Hotel.
-- Запускается PostgreSQL автоматически при первом старте контейнера:
--   docker-compose.yml монтирует файл в /docker-entrypoint-initdb.d/
-- Выполняется ОДИН РАЗ при создании тома. Повторный docker-compose up не перезапускает скрипт.
--
-- IF NOT EXISTS: идемпотентный — безопасно запускать несколько раз (не падает если схема уже есть).
-- Без этих схем Hibernate (ddl-auto: update) выдаст ошибку "schema not found" при CREATE TABLE.
--
-- Каждый микросервис работает в своей схеме — изоляция данных без физически разных БД.
-- Все сервисы подключаются к одной БД "hotel" с разными currentSchema в JDBC URL.

CREATE SCHEMA IF NOT EXISTS customer;  -- customer-service: пользователи, пароли, роли
CREATE SCHEMA IF NOT EXISTS room;      -- room-service: номера, цены, блокированные даты
CREATE SCHEMA IF NOT EXISTS booking;   -- booking-service: бронирования, доп. услуги
CREATE SCHEMA IF NOT EXISTS amenity;   -- amenity-service: справочник услуг + изображения
CREATE SCHEMA IF NOT EXISTS dining;    -- dining-service: меню, заказы
CREATE SCHEMA IF NOT EXISTS billing;   -- billing-service: счета, оплата
CREATE SCHEMA IF NOT EXISTS support;   -- support-service: сообщения чата поддержки
