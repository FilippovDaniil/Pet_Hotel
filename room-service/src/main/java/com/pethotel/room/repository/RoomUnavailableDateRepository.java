package com.pethotel.room.repository;

import com.pethotel.room.entity.RoomUnavailableDate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;

public interface RoomUnavailableDateRepository extends JpaRepository<RoomUnavailableDate, Long> {

    // @Modifying — сообщает Spring Data, что запрос изменяет данные (DELETE/UPDATE), а не читает.
    //   Без этой аннотации Spring попытается выполнить SELECT и выбросит исключение.
    //   Метод должен вызываться внутри @Transactional — здесь это обеспечивает RoomService.
    //
    // Удаляем только будущие даты (>= LocalDate.now()), а не весь период бронирования.
    // Это намеренно: прошедшие даты уже "состоялись" и удалять их не нужно —
    // они не влияют на поиск доступных номеров (запрос в RoomRepository смотрит на даты >= checkIn).
    // Диапазон "до now() + 2 года" — практический предел горизонта бронирования.
    @Modifying
    @Query("DELETE FROM RoomUnavailableDate ud WHERE ud.room.id = :roomId AND ud.date >= :from AND ud.date < :to")
    void deleteByRoomIdAndDateRange(@Param("roomId") Long roomId,
                                    @Param("from") LocalDate from,
                                    @Param("to") LocalDate to);
}
