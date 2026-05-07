package com.pethotel.support.service;

import com.pethotel.support.dto.ConversationSummaryDto;
import com.pethotel.support.dto.SupportMessageDto;
import com.pethotel.support.entity.SupportMessage;
import com.pethotel.support.repository.SupportMessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;

// @Slf4j — инъекция логгера через Lombok: log.info(...), log.warn(...), log.error(...)
// @Service — маркер слоя бизнес-логики; Spring создаёт один экземпляр (Singleton) и регистрирует в контексте.
// @RequiredArgsConstructor — Lombok генерирует конструктор для всех final-полей.
//   Spring видит один конструктор и инъецирует зависимости автоматически (Constructor Injection).
//   Это предпочтительнее @Autowired на поле — удобнее для тестирования (mock через конструктор).
@Slf4j
@Service
@RequiredArgsConstructor
public class SupportService {

    // final — поле инициализируется единожды через конструктор, потом неизменяемо.
    // Это гарантирует, что сервис всегда имеет репозиторий (fail-fast при старте).
    private final SupportMessageRepository repository;

    // ── Customer: получить свои сообщения ───────────────────────────────────

    public List<SupportMessageDto> getCustomerMessages(Long customerId) {
        // Загружаем все сообщения диалога, отсортированные по времени (старые → новые).
        // .stream() — преобразуем List<SupportMessage> в поток для обработки.
        // .map(this::toDto) — применяем метод toDto() к каждому элементу.
        // .toList() — собираем результат обратно в List (Java 16+, неизменяемый список).
        return repository.findByCustomerIdOrderByCreatedAtAsc(customerId)
                .stream()
                .map(this::toDto)
                .toList();
    }

    // ── Customer: отправить сообщение ────────────────────────────────────────

    public SupportMessageDto sendCustomerMessage(Long customerId, String customerEmail, String content) {
        // Builder-паттерн — создаём объект через цепочку вызовов вместо конструктора с 8 аргументами.
        // Читаемее: видно, какое поле какое значение получает.
        SupportMessage message = SupportMessage.builder()
                .customerId(customerId)
                .customerEmail(customerEmail)
                .senderRole("CUSTOMER")       // клиент отправляет → senderRole = CUSTOMER
                .content(content)
                .createdAt(LocalDateTime.now()) // UTC текущего момента
                .readByCustomer(true)          // клиент уже "прочитал" своё же сообщение
                .readByAdmin(false)            // admin ещё не видел
                .build();

        // repository.save() — INSERT в БД (если id == null) или UPDATE (если id задан).
        // Возвращает Entity с присвоенным id из БД.
        SupportMessage saved = repository.save(message);
        log.info("Customer {} sent support message id={}", customerId, saved.getId());
        return toDto(saved);
    }

    // ── Customer: счётчик непрочитанных ответов ──────────────────────────────

    public long getUnreadCountForCustomer(Long customerId) {
        // Считаем сообщения от ADMIN, которые клиент не прочёл (readByCustomer = false).
        // Используется для отображения бейджа с цифрой на кнопке "Поддержка".
        return repository.countByCustomerIdAndSenderRoleAndReadByCustomerFalse(customerId, "ADMIN");
    }

    // ── Customer: отметить ответы admin как прочитанные ──────────────────────

    // @Transactional — открывает транзакцию перед вызовом и закрывает после.
    // Здесь нужна потому, что репозиторный @Modifying-запрос должен выполняться
    // внутри транзакции. Если транзакция уже открыта выше — присоединяется к ней
    // (propagation = REQUIRED по умолчанию).
    @Transactional
    public void markAsReadByCustomer(Long customerId) {
        repository.markAdminMessagesAsReadByCustomer(customerId);
    }

    // ── Admin: список всех диалогов ──────────────────────────────────────────

    public List<ConversationSummaryDto> getConversationSummaries() {
        // Шаг 1: находим уникальные ID всех клиентов, у которых есть хоть одно сообщение.
        List<Long> customerIds = repository.findDistinctCustomerIds();

        return customerIds.stream()
                .map(customerId -> {
                    // Шаг 2: берём последнее сообщение диалога (для превью и времени).
                    // Optional.map() — если сообщение найдено, строим DTO; иначе возвращаем null.
                    return repository.findTopByCustomerIdOrderByCreatedAtDesc(customerId)
                            .map(last -> {
                                // Шаг 3: считаем непрочитанные для обеих сторон.
                                long unreadByAdmin = repository.countByCustomerIdAndSenderRoleAndReadByAdminFalse(customerId, "CUSTOMER");
                                long unreadByCustomer = repository.countByCustomerIdAndSenderRoleAndReadByCustomerFalse(customerId, "ADMIN");
                                return ConversationSummaryDto.builder()
                                        .customerId(customerId)
                                        .customerEmail(last.getCustomerEmail())
                                        .lastMessage(last.getContent())
                                        .lastMessageAt(last.getCreatedAt())
                                        .unreadByAdmin(unreadByAdmin)
                                        .unreadByCustomer(unreadByCustomer)
                                        .build();
                            })
                            .orElse(null); // теоретически невозможно (findDistinctCustomerIds возвращает только реальных)
                })
                // Убираем null из потока на случай гонки данных
                .filter(Objects::nonNull)
                // Сортировка: диалог с последним сообщением идёт первым (как в мессенджерах)
                .sorted(Comparator.comparing(ConversationSummaryDto::getLastMessageAt).reversed())
                .toList();
    }

    // ── Admin: полный диалог с клиентом ──────────────────────────────────────

    public List<SupportMessageDto> getAdminConversation(Long customerId) {
        return repository.findByCustomerIdOrderByCreatedAtAsc(customerId)
                .stream()
                .map(this::toDto)
                .toList();
    }

    // ── Admin: ответить клиенту ───────────────────────────────────────────────

    public SupportMessageDto sendAdminMessage(Long customerId, String content) {
        // Берём email клиента из существующих сообщений диалога.
        // Нельзя взять из запроса (admin не знает email клиента наизусть),
        // нельзя позвонить в customer-service (лишняя зависимость).
        // orElseThrow — если диалога нет (никто не писал), бросаем исключение.
        // GlobalExceptionHandler превратит его в 404 { "error": "..." }.
        String customerEmail = repository.findTopByCustomerIdOrderByCreatedAtDesc(customerId)
                .map(SupportMessage::getCustomerEmail)
                .orElseThrow(() -> new NoSuchElementException("Диалог с клиентом " + customerId + " не найден"));

        SupportMessage message = SupportMessage.builder()
                .customerId(customerId)
                .customerEmail(customerEmail)
                .senderRole("ADMIN")          // admin отвечает → senderRole = ADMIN
                .content(content)
                .createdAt(LocalDateTime.now())
                .readByCustomer(false)        // клиент ещё не видел
                .readByAdmin(true)            // admin уже "прочитал" своё же сообщение
                .build();

        SupportMessage saved = repository.save(message);
        log.info("Admin replied to customer {} message id={}", customerId, saved.getId());
        return toDto(saved);
    }

    // ── Admin: отметить сообщения клиента как прочитанные ────────────────────

    @Transactional
    public void markAsReadByAdmin(Long customerId) {
        repository.markCustomerMessagesAsReadByAdmin(customerId);
    }

    // ── Приватный метод маппинга Entity → DTO ────────────────────────────────

    // Называем "toDto" — короткое имя для internal-метода.
    // this::toDto — ссылка на метод, используемая в .map() выше.
    // Принцип: маппинг в одном месте — изменение одного поля не затрагивает несколько методов.
    private SupportMessageDto toDto(SupportMessage m) {
        return SupportMessageDto.builder()
                .id(m.getId())
                .customerId(m.getCustomerId())
                .customerEmail(m.getCustomerEmail())
                .senderRole(m.getSenderRole())
                .content(m.getContent())
                .createdAt(m.getCreatedAt())
                .readByCustomer(m.isReadByCustomer())   // boolean → isX() вместо getX()
                .readByAdmin(m.isReadByAdmin())
                .build();
    }
}
