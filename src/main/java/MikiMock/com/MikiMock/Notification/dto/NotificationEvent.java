package MikiMock.com.MikiMock.Notification.dto;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationEvent {

    private String toEmail;

    private String subject;

    private String body;
}