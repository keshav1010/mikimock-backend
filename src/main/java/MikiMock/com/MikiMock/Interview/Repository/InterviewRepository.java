package MikiMock.com.MikiMock.Interview.Repository;

import MikiMock.com.MikiMock.Interview.entity.InterviewQuestion;
import MikiMock.com.MikiMock.Interview.entity.InterviewSchedule;
import MikiMock.com.MikiMock.Interview.entity.ScheduleStatus;
import MikiMock.com.MikiMock.User.dto.InterviewDateResponse;
import MikiMock.com.MikiMock.User.entity.User;
import io.lettuce.core.dynamic.annotation.Param;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InterviewRepository extends JpaRepository<InterviewSchedule,Long> {
    @Query("""

        SELECT i
        FROM InterviewSchedule i
        WHERE i.user.id = :userId
        AND i.status = 'SCHEDULED'
        AND i.scheduledTime <= CURRENT_TIMESTAMP
        AND i.endTime > CURRENT_TIMESTAMP

    """)
    Optional<InterviewSchedule> findActiveSchedule( @Param("userId") Long userId);


    @Query("""

        SELECT i
        FROM InterviewSchedule i
        WHERE i.user.id = :userId
        AND i.status = 'SCHEDULED'
        AND i.endTime >= CURRENT_TIMESTAMP
    """)
    List<InterviewSchedule> findFutureSchedule(@Param("userId") Long userId);


    @Query("""
            SELECT a.scheduledTime FROM InterviewSchedule a
            where a.user.id = :userId AND a.status = 'SCHEDULED'
            ORDER BY a.scheduledTime ASC
            """)
    List<InterviewDateResponse> getInterviewDates(@Param("userId") Long userId);

    @Query("""
            SELECT a.topic , Count(a.topic) FROM InterviewSchedule a
            WHERE a.user.id = :userId And a.status = 'SCHEDULED'
            GROUP BY a.topic
            """)
    List<Object[]> getTopicStats(@Param("userId") Long userId);

    @Query("""
            SELECT i.level, Count(i.level) from InterviewSchedule i
            WHERE i.user.id = :userId AND i.status = 'SCHEDULED'
            GROUP BY i.level
        """)
    List<Object[]> getLevelStats(@Param("userId") Long userId);


    @Query("""
            Select i from InterviewSchedule i
            where i.user.id = :userId
            order by i.scheduledTime DESC
            """)
    Page<InterviewSchedule> getInterviewList(@Param("userId") Long userId , Pageable pageable);

    Optional<InterviewQuestion> findTopByUserAndTopicAndStatusAndSolvedProblemIsNotNullOrderByCreatedAtDesc(User user, @NotBlank(message = "Topic is required") @Size(max = 100, message = "Topic cannot exceed 100 characters") String topic, ScheduleStatus scheduleStatus);

    @Query("""
    SELECT i
    FROM InterviewSchedule i
    WHERE i.user.id = :userId
      AND i.scheduledTime <= :now
      AND i.endTime >= :now
    """)
    Optional<InterviewSchedule> getActiveInterviewByUserId(
            @Param("userId") Long userId,
            @Param("now") LocalDateTime now
    );

    Optional<InterviewSchedule> findById(UUID id);


    boolean existsByUserAndScheduledTimeAndStatusNot(User user, @NotNull(message = "Time is required") @Future(message = "Time must be in the future") LocalDateTime time, ScheduleStatus scheduleStatus);

    List<InterviewSchedule> findByscheduledTimeBetweenAndStatus(
                LocalDateTime start,
                LocalDateTime end,
                ScheduleStatus status
    );

    @Query(value = """
        SELECT COUNT(*)
        FROM interview_schedule
        WHERE status='COMPLETED'
        AND DATE(end_time)=CURRENT_DATE
        """, nativeQuery = true)
    Long getCompletedInterviewToday();

    @Query("""
            select count(i) from InterviewSchedule i
            where i.endTime < CURRENT_TIMESTAMP
            And i.status = 'SCHEDULED'
            AND FUNCTION('DATE', i.endTime) = CURRENT_DATE
            """)
    Long noShowToday();


    @Query("""
            select count(i) from InterviewSchedule i
            where i.status = 'SCHEDULED'
            """)
    Long countRunning();
}
