package MikiMock.com.MikiMock.Interview.Repository;

import MikiMock.com.MikiMock.Interview.entity.InterviewQuestion;
import io.lettuce.core.dynamic.annotation.Param;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface QuestionRepository extends JpaRepository<InterviewQuestion , Long> {

    List<InterviewQuestion> findByTopicIgnoreCaseAndLevelIgnoreCaseAndActiveTrue(@NotBlank(message = "Topic is required") @Size(max = 100, message = "Topic cannot exceed 100 characters") String topic, @NotBlank(message = "Level is required") @Size(max = 30, message = "Level cannot exceed 30 characters") String level);

    @Query(
            value = """
                    SELECT *
                    FROM interview_question_bank
                    WHERE topic = :topic
                    AND level = :level
                    AND category = :category
                    AND active = true
                    ORDER BY random()
                    LIMIT 1
                    """,
            nativeQuery = true
    )
    Optional<InterviewQuestion> findRandomByTopicLevelCategory(
            @Param("topic") String topic,
            @Param("level") String level,
            @Param("category") String category
    );
}
