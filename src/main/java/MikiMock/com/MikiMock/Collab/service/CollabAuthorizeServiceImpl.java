package MikiMock.com.MikiMock.Collab.service;


import MikiMock.com.MikiMock.Collab.dto.CollabAuthorizeResponse;
import MikiMock.com.MikiMock.Common.Exception.BusinessException;
import MikiMock.com.MikiMock.Interview.entity.Room;
import MikiMock.com.MikiMock.Room.repository.RoomParticipantRepository;
import MikiMock.com.MikiMock.Room.repository.RoomRepository;
import MikiMock.com.MikiMock.User.entity.User;
import MikiMock.com.MikiMock.User.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class CollabAuthorizeServiceImpl implements CollabAuthorizeService {

    private final RoomRepository roomRepository;

    private final RoomParticipantRepository
            participantRepository;

    private final UserRepository userRepository;

    @Override
    public CollabAuthorizeResponse authorize(
            String roomCode
    ) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (
                authentication == null
                        || !authentication.isAuthenticated()
        ) {
            throw new BusinessException(
                    "User not authenticated"
            );
        }

        String email =
                authentication.getName();

        User user =
                userRepository.findByEmail(email)
                        .orElseThrow(() ->
                                new BusinessException(
                                        "User not found"
                                )
                        );

        Room room =
                roomRepository.findByRoomCode(roomCode)
                        .orElseThrow(() ->
                                new BusinessException(
                                        "Room not found"
                                )
                        );

        boolean isParticipant =
                participantRepository
                        .findByRoomAndUser(
                                room,
                                user
                        )
                        .isPresent();

        if (!isParticipant) {
            throw new BusinessException(
                    "User is not part of this room"
            );
        }

        return CollabAuthorizeResponse.builder()
                .roomCode(roomCode)
                .userId(user.getId())
                .allowed(true)
                .build();
    }
}