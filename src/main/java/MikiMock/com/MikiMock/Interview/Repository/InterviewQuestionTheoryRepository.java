package MikiMock.com.MikiMock.Interview.Repository;

import MikiMock.com.MikiMock.Interview.entity.InterviewQuestion;
import MikiMock.com.MikiMock.Interview.entity.InterviewQuestionTheory;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;


@Repository
public interface InterviewQuestionTheoryRepository extends JpaRepository<InterviewQuestionTheory, Long> {
    @Query(
            value = """
                    SELECT id
                    FROM interview_questions_theory
                    WHERE topic = :topic
                    AND level = :level
                    AND category = :category
                    AND active = true
                    ORDER BY random()
                    LIMIT 1
                    """,
            nativeQuery = true
    )
    Optional<Long> findRandomByTopicLevelCategory(
            @Param("topic") String topic,
            @Param("level") String level,
            @Param("category") String category
    );
}
