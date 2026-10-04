package MikiMock.com.MikiMock.Question.service;

import MikiMock.com.MikiMock.Interview.Repository.QuestionRepository;
import MikiMock.com.MikiMock.Interview.entity.InterviewQuestion;
import MikiMock.com.MikiMock.Question.dto.QuestionResponse;
import lombok.RequiredArgsConstructor;
import org.aspectj.weaver.patterns.TypePatternQuestions;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class QuestionServiceImpl implements QuestionService {

    private static final String DSA_TOPIC = "DSA";

    private final QuestionRepository questionRepository;

    @Override
    @Transactional(readOnly = true)
    public List<QuestionResponse> getAllDSAQuestions() {

        return questionRepository
                .findByTopicIgnoreCase(DSA_TOPIC)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private QuestionResponse toResponse(InterviewQuestion question) {

        return new QuestionResponse(
                question.getId(),
                question.getTitle(),
                question.getCategory(),
                question.getLevel(),
                question.getCompany(),
                question.getSolve()
        );
    }
}