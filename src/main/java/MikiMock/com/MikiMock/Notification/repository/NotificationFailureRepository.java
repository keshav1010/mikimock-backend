package MikiMock.com.MikiMock.Notification.repository;

import MikiMock.com.MikiMock.Notification.entity.NotificationFailure;
import MikiMock.com.MikiMock.Notification.entity.NotificationFailureStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NotificationFailureRepository
        extends JpaRepository<NotificationFailure, Long> {

    Optional<NotificationFailure> findByEventId(
            String eventId
    );

    List<NotificationFailure> findByStatus(
            NotificationFailureStatus status
    );

    Long countByStatus(
            NotificationFailureStatus status
    );

}