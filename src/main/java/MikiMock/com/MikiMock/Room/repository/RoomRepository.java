package MikiMock.com.MikiMock.Room.repository;


import MikiMock.com.MikiMock.Interview.entity.Room;
import io.lettuce.core.dynamic.annotation.Param;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.Optional;


@Repository
public interface RoomRepository extends JpaRepository<Room,Long> {
    Optional<Room>  findByRoomCode(String roomCode);

    @Modifying
    @Query("""
    UPDATE Room r
    SET r.status = MikiMock.com.MikiMock.Interview.entity.RoomStatus.COMPLETED
    WHERE r.status = MikiMock.com.MikiMock.Interview.entity.RoomStatus.IN_PROGRESS
    AND r.expiresAt <= :now
    """)
    void updateExpiredRooms(@Param("now") LocalDateTime now);



    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT r
        FROM Room r
        WHERE r.roomCode = :roomCode
    """)
    Optional<Room> findByRoomCodeForUpdate(String roomCode);



    @Query("""
    SELECT r
    FROM Room r
    JOIN RoomParticipants rp
        ON rp.room = r
    WHERE rp.user.id = :userId
    AND r.status IN (
        MikiMock.com.MikiMock.Interview.entity.RoomStatus.IN_PROGRESS,
        MikiMock.com.MikiMock.Interview.entity.RoomStatus.ACTIVE
    )
    ORDER BY r.createdAt DESC
        """)
    Optional<Room> findActiveRoomByUserId(
            @Param("userId") Long userId
    );

}
