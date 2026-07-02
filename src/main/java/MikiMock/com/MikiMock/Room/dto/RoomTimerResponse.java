package MikiMock.com.MikiMock.Room.dto;

import MikiMock.com.MikiMock.Interview.entity.RoomStatus;
import lombok.*;

import java.time.LocalDateTime;

@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoomTimerResponse {
    private String roomCode;

    private RoomStatus status;

    private LocalDateTime startedAt;

    private LocalDateTime expiredAt;

    private LocalDateTime serverTime;

    private Long remainingSeconds;

}
