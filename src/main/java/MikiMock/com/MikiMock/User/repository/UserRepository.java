package MikiMock.com.MikiMock.User.repository;

import MikiMock.com.MikiMock.User.entity.User;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository
        extends JpaRepository<User, Long> {

    Optional<User> findByEmail(
            String email
    );

    Optional<User> findByPublicId(
            UUID publicId
    );

    boolean existsByEmail(
            String email
    );


    @Query("""
    SELECT COUNT(u)
    FROM User u
    WHERE u.createdAt >= :start
      AND u.createdAt < :end
    """)
    Long getRegisteredToday(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );


    @Query("""
        SELECT count(u) from User u
        where u.subscriptionType = 'PREMIUM'
    """)
    Long countPremiumUsers();

    @Query("""
        SELECT count(u) from User u
        where u.subscriptionType = 'FREE'
    """)
    Long countFreeUsers();


}