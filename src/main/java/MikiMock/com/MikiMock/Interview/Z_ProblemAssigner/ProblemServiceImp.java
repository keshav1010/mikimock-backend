package MikiMock.com.MikiMock.Interview.Z_ProblemAssigner;


import MikiMock.com.MikiMock.Common.Exception.BusinessException;
import MikiMock.com.MikiMock.Interview.Repository.AssignmentRepository;
import MikiMock.com.MikiMock.Interview.Repository.CodeTemplateRepository;
import MikiMock.com.MikiMock.Interview.Repository.InterviewQuestionTheoryRepository;
import MikiMock.com.MikiMock.Interview.dto.AssignedProblemResponse;
import MikiMock.com.MikiMock.Interview.entity.*;
import MikiMock.com.MikiMock.Room.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class ProblemServiceImp implements ProblemService {

    private final RoomRepository roomRepository;
    private final AssignmentRepository assignmentRepository;
    private final CodeTemplateRepository codeTemplateRepository;
    private final InterviewQuestionTheoryRepository interviewQuestionTheoryRepository;

    @Override
    @Transactional(readOnly = true)
    public AssignedProblemResponse getAssignedProblem(String roomCode, String language) {

        Room room = roomRepository.findByRoomCode(roomCode).orElseThrow(() -> new BusinessException("Room not found"));

        RoomProblemAssignment assignment = assignmentRepository.findByRoom(room).orElseThrow(() ->
                new BusinessException("Problem not assigned"));

        if (room.getRoomMode() == RoomMode.JAVA_INTERVIEW || room.getRoomMode() == RoomMode.MANAGERIAL_INTERVIEW){

            String questionsId = assignment.getQuestion().getTitle();
            List<Long> ids = Arrays.stream(questionsId.split(","))
                    .map(String::trim)
                    .map(Long::parseLong)
                    .toList();

            Map<Long, InterviewQuestionTheory> questionMap =
                    interviewQuestionTheoryRepository.findAllById(ids)
                            .stream()
                            .collect(Collectors.toMap(
                                    InterviewQuestionTheory::getId,
                                    Function.identity()
                            ));

            List<String> questionList = ids.stream()
                    .map(questionMap::get)
                    .filter(Objects::nonNull)
                    .map(InterviewQuestionTheory::getQuestion)
                    .toList();

            return AssignedProblemResponse.builder()
                    .level(room.getLevel())
                    .topic(room.getTopic())
                    .questionId(assignment.getQuestion().getId())
                    .theoryQuestions(questionList)
                    .build();
        };



        InterviewQuestion question = assignment.getQuestion();
        log.info("Error for question: {}  | language: {}" ,question.getTitle() , language);

        QuestionCodeTemplate template = codeTemplateRepository.findByQuestionAndLanguageIgnoreCase(question, language)
                        .orElseThrow(() -> new BusinessException(
                                        "Code template not found for language: " + language + question.getId()
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
}
