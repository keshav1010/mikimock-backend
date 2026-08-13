package MikiMock.com.MikiMock.Notification.service;

import MikiMock.com.MikiMock.Common.Exception.BusinessException;
import MikiMock.com.MikiMock.Notification.dto.NotificationEvent;
import MikiMock.com.MikiMock.Notification.dto.ReplayResponse;
import MikiMock.com.MikiMock.Notification.entity.NotificationFailure;
import MikiMock.com.MikiMock.Notification.entity.NotificationFailureStatus;
import MikiMock.com.MikiMock.Notification.producer.NotificationProducer;
import MikiMock.com.MikiMock.Notification.repository.NotificationFailureRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationReplayServiceImpl
        implements NotificationReplayService {

    private final NotificationFailureRepository repository;

    private final NotificationProducer notificationProducer;

    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public ReplayResponse replay(String eventId) {

        NotificationFailure failure =
                repository.findByEventId(eventId)
                        .orElseThrow(() ->
                                new BusinessException("Notification not found."));

        if (failure.getStatus() == NotificationFailureStatus.REPLAYING) {
            throw new BusinessException("Replay already in progress.");
        }

        try {

            NotificationEvent event =
                    objectMapper.readValue(
                            failure.getOriginalPayload(),
                            NotificationEvent.class
                    );

            // Mark replay in progress
            failure.setStatus(NotificationFailureStatus.REPLAYING);
            repository.save(failure);

            // Publish again
            notificationProducer.publish(event);

            log.info("Replay published successfully. eventId={}", eventId);

            return ReplayResponse.builder()
                    .eventId(eventId)
                    .message("Replay request submitted successfully.")
                    .build();

        } catch (Exception e) {

            failure.setStatus(NotificationFailureStatus.FAILED);
            repository.save(failure);

            log.error("Replay failed. eventId={}", eventId, e);

            throw new BusinessException("Replay failed.");
        }
    }
}