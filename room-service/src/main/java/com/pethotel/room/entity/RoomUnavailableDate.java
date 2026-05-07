package com.pethotel.room.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

// Одна запись = один конкретный день, когда номер занят.
// При бронировании создаётся по одной записи на каждые сутки пребывания (blockDates в RoomService).
// При отмене — записи удаляются (unblockDates).
//
// Денормализованная структура (строка на день, не периоды) упрощает SQL-запрос поиска свободных номеров:
// достаточно проверить отсутствие строк в диапазоне, без логики пересечения интервалов.
@Entity
@Table(name = "room_unavailable_dates", schema = "room")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoomUnavailableDate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // @ManyToOne — много дат принадлежат одному номеру (обратная сторона Room.unavailableDates).
    // fetch = LAZY — Room загружается из БД только при первом обращении к полю, не при загрузке даты.
    //   Это экономит запросы: когда нам нужна только дата, Room подтягивать незачем.
    // @JoinColumn — эта сторона связи владеет FK-колонкой room_id в таблице room_unavailable_dates.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    // LocalDate — дата без времени (YYYY-MM-DD); достаточно для блокировки дней.
    @Column(nullable = false)
    private LocalDate date;
}
