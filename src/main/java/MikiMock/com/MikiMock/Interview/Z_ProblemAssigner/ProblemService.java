package MikiMock.com.MikiMock.Interview.Z_ProblemAssigner;


import MikiMock.com.MikiMock.Interview.dto.AssignedProblemResponse;
import org.springframework.stereotype.Service;

@Service
public interface ProblemService {
    AssignedProblemResponse getAssignedProblem(String roomCode, String language);

//    AssignedProblemResponse getRoleChanged(String roomCode);
}
