package MikiMock.com.MikiMock.User.repository;

import MikiMock.com.MikiMock.User.entity.User;
import MikiMock.com.MikiMock.User.entity.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserProfileRepository extends JpaRepository<UserProfile, Long> {
    Optional<UserProfile> findByUserId(Long id);
    Optional<UserProfile> findByUser(User user);

    @Query("""
            SELECT Count(p) FROM UserProfile p
                WHERE p.score > :score
            """)
    Integer getRank(@Param("score") Integer score);

//    @Query("""
//
//        SELECT i
//        FROM InterviewSchedule i
//        WHERE i.user.id = :userId
//        AND i.status = 'SCHEDULED'
//        AND i.scheduledTime <= CURRENT_TIMESTAMP
//        AND i.endTime > CURRENT_TIMESTAMP
//
//    """)
}
