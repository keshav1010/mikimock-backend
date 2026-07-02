package MikiMock.com.MikiMock.CodeExecution.repository;

import MikiMock.com.MikiMock.Interview.entity.QuestionTestCase;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QuestionTestCaseRepository
        extends JpaRepository<QuestionTestCase, Long> {

    List<QuestionTestCase>
    findByQuestionIdAndActiveTrueOrderByTestOrderAsc(Long questionId);
}