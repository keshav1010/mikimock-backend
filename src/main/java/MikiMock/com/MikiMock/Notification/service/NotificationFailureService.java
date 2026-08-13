package MikiMock.com.MikiMock.Notification.service;

import MikiMock.com.MikiMock.Notification.dto.NotificationEvent;
import MikiMock.com.MikiMock.Notification.entity.NotificationFailure;

import java.util.List;

public interface NotificationFailureService {

    void saveFailure(
            NotificationEvent event,
            String payload,
            Exception exception,
            String topic,
            Integer partition,
            Long offset,
            Integer retryCount
    );

    void markResolved(
            String eventId
    );

    List<NotificationFailure> getAllFailures();

    NotificationFailure getFailure(
            String eventId
    );

}