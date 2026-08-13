package MikiMock.com.MikiMock.Interview.AgoraSessionTracking.dto;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ForceLeaveEvent {

    private String eventType;

    private String roomCode;

    private String reason;
}