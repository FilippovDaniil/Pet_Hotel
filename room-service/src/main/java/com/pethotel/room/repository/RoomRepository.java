package com.pethotel.room.repository;

import com.pethotel.room.entity.Room;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface RoomRepository extends JpaRepository<Room, Long> {

    // Кастомный JPQL-запрос: возвращает номера, которые НЕ имеют ни одной заблокированной даты
    // в заданном диапазоне и вмещают нужное количество гостей.
    //
    // Логика подзапроса:
    //   SELECT ud.room.id FROM RoomUnavailableDate ud WHERE ud.date >= :checkIn AND ud.date < :checkOut
    //   → IDs всех номеров, у которых есть хоть одна занятая дата в периоде
    //
    // Внешний запрос: берём номера, которых НЕТ в этом множестве (NOT IN).
    //   AND r.capacity >= :guests — дополнительный фильтр по вместимости.
    //
    // checkOut — exclusive: если гость выезжает 15-го, то 15-е уже свободно для следующего заезда.
    // Это стандартная гостиничная логика: [checkIn, checkOut).
    @Query("""
        SELECT r FROM Room r
        WHERE r.id NOT IN (
            SELECT ud.room.id FROM RoomUnavailableDate ud
            WHERE ud.date >= :checkIn AND ud.date < :checkOut
        )
        AND r.capacity >= :guests
    """)
    List<Room> findAvailableRooms(@Param("checkIn") LocalDate checkIn,
                                   @Param("checkOut") LocalDate checkOut,
                                   @Param("guests") int guests);
}
