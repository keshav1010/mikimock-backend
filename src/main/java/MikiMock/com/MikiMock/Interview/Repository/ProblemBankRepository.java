package MikiMock.com.MikiMock.Interview.Repository;

import MikiMock.com.MikiMock.Interview.entity.ProblemBank;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface ProblemBankRepository extends JpaRepository<ProblemBank, Long> {

//@Query("""
//        SELECT p FROM problem_bank p
//        WHERE p.topic = :topic AND p.level = :level
//        ORDER BY RANDOM()
//        LIMIT 1
//        """)

    @Query(
            value = "SELECT * FROM problem_bank WHERE topic = :topic AND level = :level ORDER BY RANDOM() LIMIT 1",
            nativeQuery = true
    )

    ProblemBank getProblem(@Param("level") String level,@Param("topic") String topic);
}
