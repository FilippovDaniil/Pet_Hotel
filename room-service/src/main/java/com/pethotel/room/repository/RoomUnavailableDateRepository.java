package com.pethotel.room.repository;

import com.pethotel.room.entity.RoomUnavailableDate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;

public interface RoomUnavailableDateRepository extends JpaRepository<RoomUnavailableDate, Long> {

    @Modifying
    @Query("DELETE FROM RoomUnavailableDate ud WHERE ud.room.id = :roomId AND ud.date >= :from AND ud.date < :to")
    void deleteByRoomIdAndDateRange(@Param("roomId") Long roomId,
                                    @Param("from") LocalDate from,
                                    @Param("to") LocalDate to);
}
