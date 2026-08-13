package MikiMock.com.MikiMock.Room.service;

import MikiMock.com.MikiMock.Common.Exception.BusinessException;
import MikiMock.com.MikiMock.Interview.entity.Room;
import MikiMock.com.MikiMock.Interview.entity.RoomParticipants;
import MikiMock.com.MikiMock.Interview.entity.RoomStatus;
import MikiMock.com.MikiMock.Room.dto.RoomPresenceEvent;
import MikiMock.com.MikiMock.Room.repository.RoomParticipantRepository;
import MikiMock.com.MikiMock.Room.repository.RoomRepository;
import MikiMock.com.MikiMock.User.entity.User;
import MikiMock.com.MikiMock.User.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class RoomPresenceServiceImp implements RoomPresenceService {
    private final RoomRepository roomRepository;

    private final RoomParticipantRepository participantRepository;

    private final UserRepository userRepository;

    private final SimpMessagingTemplate messagingTemplate;

    @Override
    @Transactional
    public void joinRoom(String roomCode) {

        User user = getAuthenticatedUser();


        Room room = roomRepository.findByRoomCode(roomCode)
                .orElseThrow(() ->
                        new BusinessException("Room not found")
                );

        RoomParticipants participant = participantRepository.findByRoomAndUser(room, user)
                        .orElseThrow(() ->
                                new BusinessException("User is not part of this room")
                        );

        if (participant.getJoinedAt() != null) {
            return;
        }


        participant.setJoinedAt(LocalDateTime.now());
        if(room.getStartAt() == null) room.setStartAt(LocalDateTime.now());


        log.info(
                "Presence join | roomCode={} | userId={} | email={}",
                room.getRoomCode(),
                user.getId(),
                user.getEmail()
        );

        log.info(
                "Participant found | participantId={} | participantUserId={} | joinedAt={}",
                participant.getId(),
                participant.getUser().getId(),
                participant.getJoinedAt()
        );

        participantRepository.saveAndFlush(participant);
        long roomCount = participantRepository.countByRoom(room);
        long joinedCount = participantRepository.countByRoomAndJoinedAt(room.getId());
        log.info("Joined Count : {}",joinedCount);
//        log.info("Room Count: {}",roomCount);

        if (joinedCount == 2 && room.getStatus() != RoomStatus.IN_PROGRESS) {
            log.info("Inside Updating room details");
            room.setStatus(RoomStatus.IN_PROGRESS);
            room.setStartAt(LocalDateTime.now());
            room.setExpiresAt(LocalDateTime.now().plusMinutes(60));
            roomRepository.save(room);
        }

        publishPresenceEvent(
                "USER_JOINED",
                room,
                user
        );
    }

    @Override
    public void leaveRoom(String roomCode) {

        User user = getAuthenticatedUser();

        Room room = roomRepository.findByRoomCode(roomCode)
                .orElseThrow(() ->
                        new BusinessException("Room not found")
                );

        RoomParticipants participant =
                participantRepository.findByRoomAndUser(room, user)
                        .orElseThrow(() ->
                                new BusinessException("User is not part of this room")
                        );

        participant.setLeftAt(LocalDateTime.now());

        participantRepository.save(participant);

        publishPresenceEvent(
                "USER_LEFT",
                room,
                user
        );
    }

    private void publishPresenceEvent(
            String event,
            Room room,
            User user
    ) {

        RoomPresenceEvent payload = RoomPresenceEvent.builder()
                        .event(event)
                        .roomCode(room.getRoomCode())
                        .userId(user.getId())
                        .fullName(user.getFullName())
                        .timestamp(LocalDateTime.now())
                        .build();

        messagingTemplate.convertAndSend(
                "/topic/room/" + room.getRoomCode() + "/presence",
                payload
        );
    }

    private User getAuthenticatedUser() {

        String email =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication()
                        .getName();

        return userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new BusinessException("User not found")
                );
    }
}
