package MikiMock.com.MikiMock.Interview.Z_ProblemAssigner;

import MikiMock.com.MikiMock.Common.Exception.BusinessException;
import MikiMock.com.MikiMock.Interview.Repository.*;
import MikiMock.com.MikiMock.Interview.dto.ScheduleRequest;
import MikiMock.com.MikiMock.Interview.entity.*;
import MikiMock.com.MikiMock.User.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

import static org.hibernate.type.descriptor.java.CoercionHelper.toLong;

@Service
@Slf4j
@RequiredArgsConstructor
public class ProblemAssignmentServiceImp implements ProblemAssignmentService {

    private final InterviewRepository interviewRepository;
    private final QuestionRepository questionRepository;
    private final AssignmentRepository assignmentRepository;
    private final InterviewQuestionTheoryRepository interviewQuestionTheoryRepository;
    private final InterviewQuestionRepository interviewQuestionRepository;


    @Override
    public InterviewQuestion assignProblemToUser(User user, ScheduleRequest request) {

        if(request.getTopic().equals("Managerial") || request.getTopic().equals("Java Development")){

            String title = findTitle(request.getTopic() , request.getLevel());



            InterviewQuestion interviewQuestion = InterviewQuestion.builder()
                    .active(true)
                    .title(title)
                    .score(toLong(10))
                    .topic(request.getTopic())
                    .level(request.getLevel())
                    .description("List of Question of " + request.getTopic())
                    .category("GENERAL")
                    .build();

            interviewQuestionRepository.save(interviewQuestion);

            return interviewQuestion;
        }

        if(user.getFreeInterviewUsed() > 0) {
            InterviewQuestion interviewQuestion = interviewRepository.findTopByUserAndTopicAndStatusAndSolvedProblemIsNotNullOrderByCreatedAtDesc(
                    user,
                    request.getTopic(),
                    ScheduleStatus.COMPLETED
            ).orElse(null);
            if(interviewQuestion != null) return interviewQuestion;
        }


        List<InterviewQuestion> questions = questionRepository.findByTopicIgnoreCaseAndLevelIgnoreCaseAndActiveTrue(request.getTopic() , request.getLevel());

        if (questions.isEmpty()) {

            throw new BusinessException(
                    "No questions available for topic "
                            + request.getTopic() +
                            " and level "
                            + request.getLevel()
            );
        }

        InterviewQuestion interviewQuestion = questions.get(ThreadLocalRandom.current().nextInt(questions.size()));

        return interviewQuestion;



    }

    @Override
    public void assignProblemToRoom(Room room, RoomParticipants participant1, RoomParticipants participant2) {

        InterviewSchedule interviewSchedule1 = interviewRepository.getActiveInterviewByUserId
                        (participant1.getUser().getId() , LocalDateTime.now())
                .orElseThrow(() -> new BusinessException("Scheduled Problem not found"));

        InterviewSchedule interviewSchedule2 = interviewRepository.getActiveInterviewByUserId
                (participant2.getUser().getId() , LocalDateTime.now())
                .orElseThrow(() -> new BusinessException("Scheduled Problem not found"));

        RoomProblemAssignment roomProblemAssignment = RoomProblemAssignment.builder()
                .room(room)
                .interviewee(participant2.getParticipantRole().equals(ParticipantRole.INTERVIEWEE)
                        ?  participant2.getUser()  :  participant1.getUser())
                .assignedAt(LocalDateTime.now())
                .active(true)
                .question(participant2.getParticipantRole().equals(ParticipantRole.INTERVIEWEE)
                        ?  interviewSchedule1.getAssignedProblem()  :  interviewSchedule2.getAssignedProblem())
                .build();

        assignmentRepository.save(roomProblemAssignment);

    }

    public void changeAssignProblemToRoom(Room room, RoomParticipants participant1, RoomParticipants participant2) {

        RoomProblemAssignment assignment = assignmentRepository.findByRoom(room).orElseThrow(() ->
                new BusinessException("Problem not assigned"));

        InterviewSchedule interviewSchedule1 = interviewRepository.getActiveInterviewByUserId
                        (participant1.getUser().getId() , LocalDateTime.now())
                .orElseThrow(() -> new BusinessException("Scheduled Problem not found"));

        InterviewSchedule interviewSchedule2 = interviewRepository.getActiveInterviewByUserId
                        (participant2.getUser().getId() , LocalDateTime.now())
                .orElseThrow(() -> new BusinessException("Scheduled Problem not found"));

        assignment.setInterviewee(participant2.getParticipantRole().equals(ParticipantRole.INTERVIEWEE)
                ?  participant2.getUser()  :  participant1.getUser());

        assignment.setQuestion(participant2.getParticipantRole().equals(ParticipantRole.INTERVIEWEE)
                ?  interviewSchedule1.getAssignedProblem()  :  interviewSchedule2.getAssignedProblem());


        assignmentRepository.save(assignment);

    }


    public InterviewQuestion getNewProblem(String topic, String level) {
        List<InterviewQuestion> questions = questionRepository.findByTopicIgnoreCaseAndLevelIgnoreCaseAndActiveTrue(topic , level);
//        log.info("List of Problems:{}",questions.toString());

        InterviewQuestion interviewQuestion = questions.get(ThreadLocalRandom.current().nextInt(questions.size()));


        return interviewQuestion;
    }



    //----------FOR THEORY
    private String findTitle(String topic , String level){
        List<String> categories = getCategoryOrder(topic);

        List<Long> questionIds = new ArrayList<>();

        for (String category : categories) {
            Long id = interviewQuestionTheoryRepository.findRandomByTopicLevelCategory(
                            normalize(topic),
                            normalize(level),
                            category
                    )
                    .orElseThrow(() ->
                            new BusinessException(
                                    "Question not found for "
                                            + topic
                                            + " "
                                            + level
                                            + " "
                                            + category
                            )
                    );
            questionIds.add(id);
        }

        return questionIds.stream()
                .sorted()
                .map(String::valueOf)
                .collect(Collectors.joining(","));
    }





    private List<String> getCategoryOrder(
            String topic
    ) {

        String normalizedTopic = normalize(topic);

        if ("MANAGERIAL".equals(normalizedTopic)) {

            return List.of(
                    "INTRODUCTION",
                    "EXPERIENCE",
                    "PROJECT",
                    "BEHAVIORAL",
                    "CONFLICT",
                    "LEADERSHIP",
                    "PRESSURE_HANDLING",
                    "COMMUNICATION",
                    "SELF_ANALYSIS",
                    "CLOSING"
            );
        }

        if ("JAVA_DEVELOPMENT".equals(normalizedTopic)) {

            return List.of(
                    "CORE_JAVA",
                    "OOPS",
                    "COLLECTIONS",
                    "STREAM_API",
                    "MULTITHREADING",
                    "JVM",
                    "SPRING",
                    "HIBERNATE",
                    "MICROSERVICES",
                    "DATABASE"
            );
        }

        throw new BusinessException(
                "Question categories not configured for topic "
                        + topic
        );
    }

    private String normalize(
            String value
    ) {

        return value
                .trim()
                .toUpperCase()
                .replace(" ", "_");
    }


}
