package MikiMock.com.MikiMock.Notification.service;

import MikiMock.com.MikiMock.Notification.dto.NotificationEvent;
import MikiMock.com.MikiMock.Notification.dto.NotificationType;
import MikiMock.com.MikiMock.Notification.producer.NotificationProducer;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SupportMailService {

    private final NotificationProducer producer;

    @Value("${spring.ses.to-support-email}")
    private String supportMail;

    public void sendFailureMail(NotificationEvent event){

        NotificationEvent mail =
                NotificationEvent.builder()

                        .eventId(
                                UUID.randomUUID().toString()
                        )

                        .notificationType(
                                NotificationType.EMAIL_FAILED
                        )

                        .toEmail(
                                supportMail
                        )

                        .subject(
                                "Notification Failed"
                        )

                        .body(
                                """
                                EventId : %s

                                User : %s

                                Subject : %s
                                """
                                        .formatted(
                                                event.getEventId(),
                                                event.getToEmail(),
                                                event.getSubject()
                                        )
                        )

                        .build();

        producer.publish(mail);
    }
}
