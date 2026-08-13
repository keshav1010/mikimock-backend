package MikiMock.com.MikiMock.Notification.consumer;

import MikiMock.com.MikiMock.Notification.dto.NotificationEvent;
import MikiMock.com.MikiMock.Notification.service.NotificationFailureService;
import MikiMock.com.MikiMock.Notification.service.SupportMailService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationDLTConsumer {

    private final ObjectMapper objectMapper;

    private final NotificationFailureService failureService;

    private final SupportMailService supportMailService;

    @KafkaListener(
            topics = "${notification.topic}-dlt",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void consume(ConsumerRecord<String, String> record) {

        try {

            String payload = record.value();

            NotificationEvent event =
                    objectMapper.readValue(
                            payload,
                            NotificationEvent.class
                    );

            failureService.saveFailure(

                    event,

                    payload,

                    new RuntimeException("Notification moved to DLT"),

                    record.topic(),

                    record.partition(),

                    record.offset(),

                    3        // replace later with configurable value
            );

            supportMailService.sendFailureMail(event);

            log.error(
                    "Notification moved to DLT. eventId={}",
                    event.getEventId()
            );

        } catch (Exception e) {

            log.error(
                    "Failed processing DLT message.",
                    e
            );
        }
    }
}