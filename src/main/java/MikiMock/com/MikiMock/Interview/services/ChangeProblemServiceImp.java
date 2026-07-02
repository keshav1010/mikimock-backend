package MikiMock.com.MikiMock.Interview.services;

import MikiMock.com.MikiMock.Common.Exception.BusinessException;
import MikiMock.com.MikiMock.Interview.Repository.AssignmentRepository;
import MikiMock.com.MikiMock.Interview.Repository.CodeTemplateRepository;
import MikiMock.com.MikiMock.Interview.Repository.InterviewRepository;
import MikiMock.com.MikiMock.Interview.Z_ProblemAssigner.ProblemAssignmentServiceImp;
import MikiMock.com.MikiMock.Interview.dto.AssignedProblemResponse;
import MikiMock.com.MikiMock.Interview.dto.FutureScheduledInterviews;
import MikiMock.com.MikiMock.Interview.dto.ProblemChangedEvent;
import MikiMock.com.MikiMock.Interview.entity.*;
import MikiMock.com.MikiMock.Room.repository.RoomParticipantRepository;
import MikiMock.com.MikiMock.Room.repository.RoomRepository;
import MikiMock.com.MikiMock.User.entity.User;
import MikiMock.com.MikiMock.User.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;


@Service
@RequiredArgsConstructor
@Slf4j
public class ChangeProblemServiceImp implements ChangeProblemService{

    private final UserRepository userRepository;
    private final RoomRepository roomRepository;
    private final RoomParticipantRepository participantRepository;
    private final AssignmentRepository assignmentRepository;
    private final ProblemAssignmentServiceImp problemAssignmentServiceimp;
    private final CodeTemplateRepository codeTemplateRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final InterviewRepository interviewRepository;


    @Override
    public AssignedProblemResponse changeRoomProblem(String roomCode, String language) {
        User user = getAuthenticatedUser();

        Room room = roomRepository.findByRoomCode(roomCode).orElseThrow(() ->
                                new BusinessException("Room not found"));

        RoomParticipants participant = participantRepository.findByRoomAndUser(room, user).orElseThrow(() ->
                        new BusinessException("User is not part of this room"));

        if (participant.getParticipantRole() != ParticipantRole.INTERVIEWER)
            {
            throw new BusinessException("Only interviewer can change problem");
        }

        RoomProblemAssignment assignment = assignmentRepository.findByRoom(room)
                        .orElseThrow(() -> new BusinessException("Problem assignment not found"));

        InterviewQuestion newProblem = problemAssignmentServiceimp.getNewProblem(room.getTopic(), room.getLevel());

        assignment.setQuestion(newProblem);
        assignmentRepository.save(assignment);

        AssignedProblemResponse response = toResponse(newProblem, language);

        messagingTemplate.convertAndSend(
                "/topic/room/" + roomCode + "/problem",
                ProblemChangedEvent.builder()
                        .eventType("PROBLEM_CHANGED")
                        .problem(response)
                        .build()
        );


        return  response;
    }

    @Override
    public FutureScheduledInterviews changeUserProblem(String language, UUID scheduledInterviewId) {
        User user = getAuthenticatedUser();
        log.info("ScheduledInterviewId =  {}",scheduledInterviewId);

        InterviewSchedule interviewSchedule = interviewRepository.findById(scheduledInterviewId).orElseThrow(
                () -> new BusinessException("Scheduled Interview not found")
        );
        LocalDateTime now = LocalDateTime.now();

        if(now.isAfter(interviewSchedule.getScheduledTime().minusMinutes(15))) throw new BusinessException("Problem not changed, Interview start in 15 min.");

        InterviewQuestion newProblem = problemAssignmentServiceimp.getNewProblem(interviewSchedule.getTopic(), interviewSchedule.getLevel());

        interviewSchedule.setAssignedProblem(newProblem);
        interviewRepository.save(interviewSchedule);

        FutureScheduledInterviews futureScheduledInterviews = FutureScheduledInterviews.builder()
                .id(interviewSchedule.getId())
                .problem(newProblem.getTitle())
                .topic(interviewSchedule.getTopic())
                .level(interviewSchedule.getLevel())
                .scheduledTime(interviewSchedule.getScheduledTime())
                .endTime(interviewSchedule.getEndTime())
                .build();

        return futureScheduledInterviews;

    }

    private AssignedProblemResponse toResponse(InterviewQuestion newProblem, String language) {

        InterviewQuestion question = newProblem;

        QuestionCodeTemplate template = codeTemplateRepository.findByQuestionAndLanguageIgnoreCase(question, language)
                .orElseThrow(() -> new BusinessException(
                        "Code template not found for language: " + language
                ));


        return AssignedProblemResponse.builder()
                .questionId(question.getId())
                .title(question.getTitle())
                .description(question.getDescription())
                .topic(question.getTopic())
                .level(question.getLevel())
                .examples(question.getExamples())
                .constraints(question.getConstraints())
                .language(template.getLanguage())
                .starterCode(template.getStarterCode())
                .build();
    }


    private User getAuthenticatedUser() {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated() ||authentication instanceof AnonymousAuthenticationToken){
            throw new BusinessException("User not authorized");
        }

        String email = authentication.getName();
        return userRepository.findByEmail(email).orElseThrow(() -> new BusinessException("User not found"));
    }
}
