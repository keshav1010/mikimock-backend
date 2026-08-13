package MikiMock.com.MikiMock.Notification.producer;

import MikiMock.com.MikiMock.Notification.dto.NotificationEvent;
//import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.util.concurrent.CompletableFuture;


//@Component
//@RequiredArgsConstructor
//@Slf4j
//public class NotificationProducer {
//
//    private final KafkaTemplate<String, String> kafkaTemplate;
//
//    private final ObjectMapper objectMapper;
//
//    @Value("${notification.topic}")
//    private String topic;
//
//    public void publish(NotificationEvent event) {
//
//        try {
//
//            String payload =
//                    objectMapper.writeValueAsString(event);
//
//            CompletableFuture<SendResult<String, String>> future =
//                    kafkaTemplate.send(
//                            topic,
//                            event.getEventId(),
//                            payload
//                    );
//
//            future.whenComplete((result, ex) -> {
//
//                if (ex != null) {
//
//                    log.error(
//                            "Notification publish failed | eventId={} | email={}",
//                            event.getEventId(),
//                            event.getToEmail(),
//                            ex
//                    );
//
//                } else {
//
//                    log.info(
//                            "Notification published | eventId={} | partition={} | offset={}",
//                            event.getEventId(),
//                            result.getRecordMetadata().partition(),
//                            result.getRecordMetadata().offset()
//                    );
//
//                }
//
//            });
//
//        } catch (Exception e) {
//
//            log.error(
//                    "Unable to serialize notification | eventId={}",
//                    event.getEventId(),
//                    e
//            );
//
//            throw new RuntimeException(e);
//
//        }
//
//    }
//
//}

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;

    private final ObjectMapper objectMapper;

    @Value("${notification.topic}")
    private String topic;

    public void publish(NotificationEvent event) {

        try {

            String message = objectMapper.writeValueAsString(event);

            kafkaTemplate.send(topic, message)
                    .whenComplete((result, ex) -> {

                        if (ex == null) {

                            log.info(
                                    "Notification published for {}",
                                    event.getToEmail()
                            );

                        } else {

                            log.error(
                                    "Failed publishing notification",
                                    ex
                            );
                        }
                    });

        } catch (Exception e) {

            throw new RuntimeException(e);
        }
    }
}