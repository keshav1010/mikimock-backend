package MikiMock.com.MikiMock.Interview.services;

import MikiMock.com.MikiMock.Common.Exception.BusinessException;
import MikiMock.com.MikiMock.Interview.dto.RoomContextResponse;
import MikiMock.com.MikiMock.Interview.entity.Room;
import MikiMock.com.MikiMock.Interview.entity.RoomParticipants;
import MikiMock.com.MikiMock.Room.repository.RoomParticipantRepository;
import MikiMock.com.MikiMock.Room.repository.RoomRepository;
import MikiMock.com.MikiMock.User.entity.User;
import MikiMock.com.MikiMock.User.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RoomContextServiceImpl implements RoomContextService {

    private final RoomRepository roomRepository;

    private final RoomParticipantRepository participantRepository;

    private final UserRepository userRepository;

    @Override
    public RoomContextResponse getRoomContext(String roomCode) {

        User user = getAuthenticatedUser();

        Room room = roomRepository.findByRoomCode(roomCode)
                        .orElseThrow(() ->
                                new BusinessException(
                                        "Room not found"
                                )
                        );

        RoomParticipants me = participantRepository.findByRoomAndUser(room, user).orElseThrow(() ->
                        new BusinessException("User is not part of this room")
                );

        List<RoomParticipants> participants = participantRepository.findByRoom(room);

        RoomParticipants partner = participants.stream()
                        .filter(p -> !p.getUser()
                                        .getId()
                                        .equals(user.getId())
                        )
                        .findFirst()
                        .orElse(null);

        return RoomContextResponse.builder()
                .roomCode(room.getRoomCode())
                .roomMode(room.getRoomMode())
                .myRole(me.getParticipantRole())
                .language(room.getCurrentLanguage())
                .partnerRole(partner != null? partner.getParticipantRole() : null)
                .topic(room.getTopic())
                .level(room.getLevel())
                .build();
    }

    private User getAuthenticatedUser() {

        String email =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication()
                        .getName();

        return userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new BusinessException(
                                "User not found"
                        )
                );
    }
}