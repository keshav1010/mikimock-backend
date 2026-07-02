package MikiMock.com.MikiMock.Room.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoomPresenceEvent {

    private String event;

    private String roomCode;

    private Long userId;

    private String fullName;

    private LocalDateTime timestamp;
}