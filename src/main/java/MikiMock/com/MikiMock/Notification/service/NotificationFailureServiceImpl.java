package MikiMock.com.MikiMock.Notification.service;

import MikiMock.com.MikiMock.Notification.dto.NotificationEvent;
import MikiMock.com.MikiMock.Notification.entity.NotificationFailure;
import MikiMock.com.MikiMock.Notification.entity.NotificationFailureStatus;
import MikiMock.com.MikiMock.Notification.repository.NotificationFailureRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationFailureServiceImpl implements NotificationFailureService {

    private final NotificationFailureRepository repository;

    @Override
    public void saveFailure(
            NotificationEvent event,
            String payload,
            Exception exception,
            String topic,
            Integer partition,
            Long offset,
            Integer retryCount
    ) {

        if (repository.findByEventId(event.getEventId()).isPresent()) {

            log.warn("Failure already exists. eventId={}", event.getEventId());

            return;
        }

        NotificationFailure failure = NotificationFailure.builder()

                .eventId(event.getEventId())
                .notificationType(event.getNotificationType())
                .toEmail(event.getToEmail())
                .subject(event.getSubject())
                .body(event.getBody())

                .topic(topic)
                .partitionNo(partition)
                .offsetNo(offset)

                .retryCount(retryCount)

                .exceptionClass(exception.getClass().getName())
                .exceptionMessage(exception.getMessage())
                .stackTrace(ExceptionUtils.getStackTrace(exception))

                .originalPayload(payload)

                .status(NotificationFailureStatus.FAILED)

                .build();

        repository.save(failure);

        log.error("Notification saved in failure table. eventId={}", event.getEventId());
    }


    @Override
    public void markResolved(String eventId) {

        repository.findByEventId(eventId)

                .ifPresent(failure -> {

                    failure.setStatus(
                            NotificationFailureStatus.RESOLVED
                    );

                    repository.save(failure);

                    log.info(
                            "Notification resolved. eventId={}",
                            eventId
                    );

                });
    }

    @Override
    public List<NotificationFailure> getAllFailures() {

        return repository.findAll();

    }

    @Override
    public NotificationFailure getFailure(
            String eventId
    ) {

        return repository.findByEventId(eventId)

                .orElseThrow();

    }

}