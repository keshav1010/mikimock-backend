package MikiMock.com.MikiMock.Interview.Repository;

import MikiMock.com.MikiMock.Interview.entity.InterviewQuestion;
import MikiMock.com.MikiMock.Interview.entity.QuestionCodeTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;


public interface CodeTemplateRepository extends JpaRepository<QuestionCodeTemplate , Long> {
    Optional<QuestionCodeTemplate> findByQuestionAndLanguageIgnoreCase(InterviewQuestion question, String language);

    Optional<QuestionCodeTemplate> findByQuestionIdAndLanguageIgnoreCase(
            Long questionId,
            String language
    );
}
