package MikiMock.com.MikiMock.Room.service;

import MikiMock.com.MikiMock.Room.dto.RoomTimerResponse;
import org.springframework.stereotype.Service;


@Service
public interface RoomTimerService {
    RoomTimerResponse getTimer(String roomCode);
}
