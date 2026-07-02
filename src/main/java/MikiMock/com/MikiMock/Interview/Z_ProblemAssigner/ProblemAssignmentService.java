package MikiMock.com.MikiMock.Interview.Z_ProblemAssigner;

import MikiMock.com.MikiMock.Interview.dto.ScheduleRequest;
import MikiMock.com.MikiMock.Interview.entity.InterviewQuestion;
import MikiMock.com.MikiMock.Interview.entity.Room;
import MikiMock.com.MikiMock.Interview.entity.RoomParticipants;
import MikiMock.com.MikiMock.User.entity.User;
import org.springframework.stereotype.Service;

@Service
public interface ProblemAssignmentService {
    InterviewQuestion assignProblemToUser(User user, ScheduleRequest request);

    void assignProblemToRoom(Room room, RoomParticipants participant1, RoomParticipants participant2);




}
