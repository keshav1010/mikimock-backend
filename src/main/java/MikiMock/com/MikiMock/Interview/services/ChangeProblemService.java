package MikiMock.com.MikiMock.Interview.services;


import MikiMock.com.MikiMock.Interview.dto.AssignedProblemResponse;
import MikiMock.com.MikiMock.Interview.dto.FutureScheduledInterviews;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public interface ChangeProblemService {
    AssignedProblemResponse changeRoomProblem(String roomCode, String language);

    FutureScheduledInterviews changeUserProblem(String language, UUID scheduledInterviewId);
}
