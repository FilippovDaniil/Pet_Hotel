package com.pethotel.room.entity;

import com.pethotel.common.enums.RoomClass;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

// Сущность номера отеля. Связана с RoomUnavailableDate через @OneToMany.
@Entity
@Table(name = "rooms", schema = "room")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Room {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // unique = true → UNIQUE constraint в PostgreSQL; одинаковые номера комнат запрещены.
    @Column(unique = true, nullable = false)
    private String roomNumber;

    // EnumType.STRING — хранить "ORDINARY"/"MIDDLE"/"PREMIUM" вместо числового индекса.
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RoomClass roomClass;

    @Column(nullable = false)
    private Integer capacity;

    // BigDecimal — тип для денежных величин: точная арифметика без ошибок округления float/double.
    @Column(nullable = false)
    private BigDecimal pricePerNight;

    private String description;

    // @OneToMany — один номер имеет много "недоступных дат".
    // mappedBy = "room" — владелец связи находится в RoomUnavailableDate (там @JoinColumn).
    //   Hibernate не создаёт дополнительную связующую таблицу — FK лежит в room_unavailable_dates.
    // cascade = ALL — операции с Room каскадируются на даты: удалили Room → удалились и все его даты.
    // orphanRemoval = true — если дату убрали из коллекции Java-кодом, Hibernate её удалит из БД.
    //   Это нужно для blockDates/unblockDates: работаем с коллекцией, не пишем DELETE вручную.
    @OneToMany(mappedBy = "room", cascade = CascadeType.ALL, orphanRemoval = true)
    // @Builder.Default — без этой аннотации @Builder оставил бы поле null вместо пустого списка.
    // Нам нужен ArrayList, чтобы Hibernate мог добавлять/убирать элементы через прокси-коллекцию.
    @Builder.Default
    private List<RoomUnavailableDate> unavailableDates = new ArrayList<>();
}
