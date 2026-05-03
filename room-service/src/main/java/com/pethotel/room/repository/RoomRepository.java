package com.pethotel.room.repository;

import com.pethotel.room.entity.Room;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface RoomRepository extends JpaRepository<Room, Long> {

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
