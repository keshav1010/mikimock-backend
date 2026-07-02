package MikiMock.com.MikiMock.Interview.Repository;

import MikiMock.com.MikiMock.Interview.dto.AssignedProblemResponse;
import MikiMock.com.MikiMock.Interview.entity.Room;
import MikiMock.com.MikiMock.Interview.entity.RoomProblemAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;


@Repository
public interface AssignmentRepository extends JpaRepository<RoomProblemAssignment, Long> {
     Optional<RoomProblemAssignment> findByRoom(Room room);

//     AssignedProblemResponse getRoomProblem(String roomCode);
}
