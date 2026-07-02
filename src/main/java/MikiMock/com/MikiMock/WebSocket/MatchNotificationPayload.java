package MikiMock.com.MikiMock.WebSocket;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MatchNotificationPayload {

    private String event;

    private String roomCode;

    private String message;
}