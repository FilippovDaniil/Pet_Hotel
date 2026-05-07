package com.pethotel.support.repository;

import com.pethotel.support.entity.SupportMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

// JpaRepository<SupportMessage, Long> — Spring Data автоматически генерирует реализацию этого интерфейса.
// Первый параметр — Entity, второй — тип первичного ключа.
// Из коробки доступны: save(), findById(), findAll(), deleteById(), count() и др.
// Ничего писать руками не нужно — Spring создаёт прокси-класс во время старта приложения.
public interface SupportMessageRepository extends JpaRepository<SupportMessage, Long> {

    // Derived Query (запрос из имени метода) — Spring Data парсит имя и генерирует SQL.
    // findByCustomerId         → WHERE customer_id = ?
    // OrderByCreatedAtAsc      → ORDER BY created_at ASC
    // Итого: SELECT * FROM support.messages WHERE customer_id = ? ORDER BY created_at ASC
    List<SupportMessage> findByCustomerIdOrderByCreatedAtAsc(Long customerId);

    // findTop1...Desc — LIMIT 1 с сортировкой по убыванию = последнее сообщение диалога.
    // Optional<> — явно сигнализирует: может не быть ни одного сообщения (новый клиент).
    Optional<SupportMessage> findTopByCustomerIdOrderByCreatedAtDesc(Long customerId);

    // Нестандартный JPQL-запрос — Spring Data не умеет вывести "DISTINCT по полю" из имени метода.
    // JPQL (Java Persistence Query Language) похож на SQL, но работает с Entity, а не таблицами.
    // m.customerId — поле Java-класса SupportMessage, не колонка БД.
    // Результат: список уникальных ID всех клиентов, у которых есть хоть одно сообщение.
    @Query("SELECT DISTINCT m.customerId FROM SupportMessage m")
    List<Long> findDistinctCustomerIds();

    // Derived Query с тремя условиями:
    // countBy...               → SELECT COUNT(*)
    // CustomerId = ?           → WHERE customer_id = ?
    // SenderRole = ?           → AND sender_role = ?
    // ReadByAdminFalse         → AND read_by_admin = false
    // Используется для подсчёта непрочитанных сообщений от клиента (которые admin не видел).
    long countByCustomerIdAndSenderRoleAndReadByAdminFalse(Long customerId, String senderRole);

    // Аналогично — непрочитанные ответы admin (которые клиент не видел).
    long countByCustomerIdAndSenderRoleAndReadByCustomerFalse(Long customerId, String senderRole);

    // @Modifying — помечает запрос как UPDATE/DELETE (не SELECT).
    // Без этой аннотации Spring Data считает @Query запросом на чтение и бросит исключение.
    // @Transactional — UPDATE требует активной транзакции. Она открывается здесь,
    // на уровне репозитория, если её нет выше по стеку вызовов.
    // @Param("customerId") — именованный параметр: :customerId в JPQL → значение аргумента.
    @Modifying
    @Transactional
    @Query("UPDATE SupportMessage m SET m.readByAdmin = true WHERE m.customerId = :customerId AND m.senderRole = 'CUSTOMER'")
    void markCustomerMessagesAsReadByAdmin(@Param("customerId") Long customerId);

    // Аналог — отмечаем ответы admin как прочитанные клиентом.
    // Вызывается когда клиент открывает страницу поддержки (после загрузки сообщений).
    @Modifying
    @Transactional
    @Query("UPDATE SupportMessage m SET m.readByCustomer = true WHERE m.customerId = :customerId AND m.senderRole = 'ADMIN'")
    void markAdminMessagesAsReadByCustomer(@Param("customerId") Long customerId);
}
