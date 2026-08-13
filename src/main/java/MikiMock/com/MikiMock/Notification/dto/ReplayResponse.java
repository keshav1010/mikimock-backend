package MikiMock.com.MikiMock.Notification.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReplayResponse {

    private String eventId;

    private String status;

    private String message;

}