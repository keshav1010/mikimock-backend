package MikiMock.com.MikiMock.Interview.services;


import MikiMock.com.MikiMock.Interview.dto.JoinRoomResponse;
import org.springframework.stereotype.Service;

@Service
public interface RoomServiceInt {
    JoinRoomResponse joinExistingRoom(String roomCode, String language);
    void changeLanguage(
            String roomCode,
            String language
    );
}
