package MikiMock.com.MikiMock.Notification.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
public class NotificationEvent {

    @Builder.Default
    private String eventId = UUID.randomUUID().toString();

    private NotificationType notificationType;

    private String toEmail;

    private String subject;

    private String body;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}