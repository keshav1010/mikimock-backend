package MikiMock.com.MikiMock.Interview.Repository;

import MikiMock.com.MikiMock.Interview.entity.InterviewQuestion;
import MikiMock.com.MikiMock.Interview.entity.QuestionCodeTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;


@Repository
public interface QuestionCodeTemplateRepository
        extends JpaRepository<QuestionCodeTemplate, Long> {

    Optional<QuestionCodeTemplate> findByQuestionAndLanguageIgnoreCase(InterviewQuestion question,
            String language
    );
}