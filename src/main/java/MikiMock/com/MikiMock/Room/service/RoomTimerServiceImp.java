package MikiMock.com.MikiMock.Room.service;


import MikiMock.com.MikiMock.Common.Exception.BusinessException;
import MikiMock.com.MikiMock.Interview.entity.Room;
import MikiMock.com.MikiMock.Interview.entity.RoomStatus;
import MikiMock.com.MikiMock.Room.dto.RoomTimerResponse;
import MikiMock.com.MikiMock.Room.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;

@Service
@Slf4j
@RequiredArgsConstructor
public class RoomTimerServiceImp implements RoomTimerService{

    private final RoomRepository roomRepository;

    @Override
    public RoomTimerResponse getTimer(String roomCode) {
        Room room = roomRepository.findByRoomCode(roomCode).orElseThrow(
                () -> new BusinessException("Room not found"));

        LocalDateTime now = LocalDateTime.now();

        Long remainingSeconds = null;
        if(room.getExpiresAt() != null){
            remainingSeconds = Math.max(0, Duration.between(now , room.getExpiresAt()).getSeconds());
        }
        log.info("Timer's End time : {}",remainingSeconds);

        return RoomTimerResponse.builder()
                .roomCode(room.getRoomCode())
                .status(RoomStatus.IN_PROGRESS)
                .startedAt(room.getStartAt())
                .expiredAt(room.getExpiresAt())
                .serverTime(now)
                .remainingSeconds(remainingSeconds)
                .build();
    }
}
