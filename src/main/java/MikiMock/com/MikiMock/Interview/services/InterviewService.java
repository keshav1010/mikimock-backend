package MikiMock.com.MikiMock.Interview.services;

import MikiMock.com.MikiMock.Interview.dto.CancelScheduledInterview;
import MikiMock.com.MikiMock.Interview.dto.FutureScheduledInterviews;
import MikiMock.com.MikiMock.Interview.dto.JoinInterviewResponse;
import MikiMock.com.MikiMock.Interview.dto.ScheduleRequest;
import jakarta.validation.Valid;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public interface InterviewService {

    ScheduleRequest scheduleInterview(@Valid ScheduleRequest request);

    JoinInterviewResponse joinInterview();

    List<FutureScheduledInterviews> getFutureScheduledInterviews();

    CancelScheduledInterview cancelScheduledInterview(UUID scheduledInterviewId);
}
