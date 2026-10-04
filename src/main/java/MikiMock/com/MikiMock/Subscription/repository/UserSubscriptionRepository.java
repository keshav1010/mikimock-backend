package MikiMock.com.MikiMock.Subscription.repository;

import MikiMock.com.MikiMock.Subscription.entity.UserSubscription;
import MikiMock.com.MikiMock.Subscription.entity.UserSubscriptionStatus;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface UserSubscriptionRepository
        extends JpaRepository<UserSubscription, Long> {

    Optional<UserSubscription> findByPaymentId(Long paymentId);

    @Query("""
        SELECT MAX(s.expiresAt)
        FROM UserSubscription s
        WHERE s.user.id = :userId
          AND s.status IN :statuses
          AND s.expiresAt > :now
        """)
    Optional<LocalDateTime> findLatestExpiry(
            @Param("userId") Long userId,
            @Param("statuses")
            List<UserSubscriptionStatus> statuses,
            @Param("now") LocalDateTime now
    );

    @Query("""
        SELECT COUNT(s) > 0
        FROM UserSubscription s
        WHERE s.user.id = :userId
          AND s.status = :status
          AND s.startsAt <= :now
          AND s.expiresAt > :now
        """)
    boolean hasValidSubscription(
            @Param("userId") Long userId,
            @Param("status") UserSubscriptionStatus status,
            @Param("now") LocalDateTime now
    );

    @Query("""
        SELECT DISTINCT s.user.id
        FROM UserSubscription s
        WHERE (
            s.status = :activeStatus
            AND s.expiresAt <= :now
        )
        OR (
            s.status = :scheduledStatus
            AND s.startsAt <= :now
        )
        ORDER BY s.user.id
        """)
    List<Long> findUsersNeedingLifecycleUpdate(
            @Param("activeStatus")
            UserSubscriptionStatus activeStatus,
            @Param("scheduledStatus")
            UserSubscriptionStatus scheduledStatus,
            @Param("now") LocalDateTime now,
            Pageable pageable
    );

    @Query("""
        SELECT s
        FROM UserSubscription s
        WHERE s.user.id = :userId
          AND s.status IN :statuses
        ORDER BY s.startsAt ASC
        """)
    List<UserSubscription> findLifecycleSubscriptions(
            @Param("userId") Long userId,
            @Param("statuses")
            List<UserSubscriptionStatus> statuses
    );
}