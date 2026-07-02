package MikiMock.com.MikiMock.Room.service;

import org.springframework.stereotype.Service;

@Service
public interface RoomPresenceService {
    void joinRoom(String roomCode);

    void leaveRoom(String roomCode);
}
