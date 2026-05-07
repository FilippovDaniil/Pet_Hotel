package com.pethotel.customer.repository;

import com.pethotel.customer.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

// JpaRepository<Customer, Long> — Spring Data JPA автоматически генерирует реализацию этого интерфейса.
// Параметры: <тип_сущности, тип_первичного_ключа>.
// "Из коробки" доступны: save(), findById(), findAll(), delete(), count() и другие CRUD-методы.
// Никакого SQL писать не нужно — Spring создаёт прокси-класс во время старта приложения.
public interface CustomerRepository extends JpaRepository<Customer, Long> {

    // Derived Query — Spring Data анализирует имя метода и генерирует JPQL:
    //   SELECT c FROM Customer c WHERE c.email = :email
    // Optional<Customer> — возвращаем Optional вместо Customer или null:
    //   это заставляет вызывающий код явно обработать "не найдено" через .orElseThrow().
    Optional<Customer> findByEmail(String email);

    // Генерирует: SELECT COUNT(*) FROM Customer c WHERE c.email = :email > 0
    // Используется в CustomerService.register() для быстрой проверки уникальности email
    // до попытки INSERT (избегаем UniqueConstraintViolationException из БД).
    boolean existsByEmail(String email);
}
