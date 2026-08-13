package MikiMock.com.MikiMock.Interview.AgoraSessionTracking.repository;


import MikiMock.com.MikiMock.Interview.AgoraSessionTracking.entity.AgoraSession;
import MikiMock.com.MikiMock.Interview.AgoraSessionTracking.entity.AgoraSessionStatus;
import MikiMock.com.MikiMock.Interview.entity.Room;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AgoraSessionRepository
        extends JpaRepository<AgoraSession, Long> {

    /**
     * One Room
     * ->
     * One Agora Session
     */
    Optional<AgoraSession> findByRoom(Room room);

    /**
     * Active Session of a Room
     */
    Optional<AgoraSession> findByRoomAndStatus(
            Room room,
            AgoraSessionStatus status
    );

    /**
     * All Active Sessions
     * (Scheduler)
     */
    List<AgoraSession> findByStatus(
            AgoraSessionStatus status
    );

    /**
     * Scheduler
     *
     * ACTIVE
     * &&
     * Interview Time Expired
     */
    List<AgoraSession> findByStatusAndStartedAtBefore(
            AgoraSessionStatus status,
            LocalDateTime startedAt
    );

    /**
     * Admin
     *
     * Today's ACTIVE sessions
     */
    @Query("""
        SELECT a
        FROM AgoraSession a
        WHERE a.status = 'ACTIVE'
        AND a.createdAt >= :startOfDay
        AND a.createdAt < :endOfDay
        """)
    List<AgoraSession> findActiveSessions(
            @Param("startOfDay") LocalDateTime startOfDay,
            @Param("endOfDay") LocalDateTime endOfDay
    );

    /**
     * Dashboard
     */
    Long countByStatus(
            AgoraSessionStatus status
    );

}