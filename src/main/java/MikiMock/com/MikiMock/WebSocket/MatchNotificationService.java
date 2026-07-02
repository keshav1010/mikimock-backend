package MikiMock.com.MikiMock.WebSocket;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MatchNotificationService {

    private final SimpMessagingTemplate messagingTemplate;

    public void notifyMatchFound(
            Long userId,
            String roomCode
    ) {

        messagingTemplate.convertAndSend(

                "/topic/match/" + userId,

                MatchNotificationPayload.builder()
                        .event("MATCH_FOUND")
                        .roomCode(roomCode)
                        .message("Partner Found")
                        .build()
        );
    }
}