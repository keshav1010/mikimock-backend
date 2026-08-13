package MikiMock.com.MikiMock.Notification.consumer;

import MikiMock.com.MikiMock.Notification.cache.NotificationIdempotencyService;
import MikiMock.com.MikiMock.Notification.dto.NotificationEvent;
import MikiMock.com.MikiMock.Notification.service.EmailService;
import MikiMock.com.MikiMock.Notification.service.NotificationFailureService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationConsumer {

    private final EmailService emailService;

    private final ObjectMapper objectMapper;

    private final NotificationIdempotencyService notificationIdempotencyService;

    private final NotificationFailureService notificationFailureService;

    @KafkaListener(
            topics = "${notification.topic}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void consume(String message) throws Exception {

        NotificationEvent event =
                objectMapper.readValue(
                        message,
                        NotificationEvent.class
                );

        // Validation
        if (event.getEventId() == null
                || event.getToEmail() == null
                || event.getSubject() == null
                || event.getBody() == null) {

            log.error("Invalid notification received.");
            return;
        }

        // Idempotency Check
        if (notificationIdempotencyService.isProcessed(event.getEventId())) {

            log.info("Duplicate notification skipped. eventId={}",
                    event.getEventId());

            return;
        }

        // Send Email
        emailService.sendMail(
                event.getToEmail(),
                event.getSubject(),
                event.getBody()
        );

        // Mark as processed in Redis
        notificationIdempotencyService.markProcessed(
                event.getEventId()
        );

        // If this notification was replayed from DLT,
        // mark it as RESOLVED.
        notificationFailureService.markResolved(
                event.getEventId()
        );

        log.info("Notification processed successfully. eventId={}",
                event.getEventId());
    }
}