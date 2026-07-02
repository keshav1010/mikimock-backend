package MikiMock.com.MikiMock.Interview.services;

import MikiMock.com.MikiMock.Interview.dto.RoomContextResponse;
import org.springframework.stereotype.Service;

@Service
public interface RoomContextService {
    RoomContextResponse getRoomContext(String roomCode);
}
