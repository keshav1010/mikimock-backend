package MikiMock.com.MikiMock.Room.repository;

import MikiMock.com.MikiMock.Interview.entity.Room;
import MikiMock.com.MikiMock.Interview.entity.RoomParticipants;
import MikiMock.com.MikiMock.User.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoomParticipantRepository extends JpaRepository<RoomParticipants,Long> {
    Optional<RoomParticipants> findByRoomAndUser(Room room, User user);

    @Query("""
        select Count(r) from RoomParticipants r
        where r.room.id = :roomId And r.joinedAt IS NOT NULL
        
    """)
    long countByRoomAndJoinedAt(@Param("roomId") Long roomId);

    List<RoomParticipants> findByRoom(Room room);

    long countByRoom(Room room);
}
