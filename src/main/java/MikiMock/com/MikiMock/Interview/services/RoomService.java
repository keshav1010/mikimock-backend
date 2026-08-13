package MikiMock.com.MikiMock.Interview.services;


import MikiMock.com.MikiMock.Common.Exception.BusinessException;
import MikiMock.com.MikiMock.Interview.Repository.AssignmentRepository;
import MikiMock.com.MikiMock.Interview.Z_ProblemAssigner.ProblemAssignmentServiceImp;
import MikiMock.com.MikiMock.Interview.Z_ProblemAssigner.ProblemServiceImp;
import MikiMock.com.MikiMock.Interview.dto.*;
import MikiMock.com.MikiMock.Interview.entity.*;
import MikiMock.com.MikiMock.Room.repository.RoomParticipantRepository;
import MikiMock.com.MikiMock.Room.repository.RoomRepository;
import MikiMock.com.MikiMock.User.entity.User;
import MikiMock.com.MikiMock.User.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoomService implements RoomServiceInt{
    private final AgoraTokenService agoraTokenService;
    private final UserRepository userRepository;
    private final RoomRepository roomRepository;
    private final RoomParticipantRepository roomParticipantRepository;
    private final RoomContextServiceImpl roomContextService;
    private final ProblemServiceImp problemServiceImp;
    private final SimpMessagingTemplate messagingTemplate;
    private final AssignmentRepository assignmentRepository;
    private final ProblemAssignmentServiceImp problemAssignmentServiceImp;

    @Value("${agora.app-id}")
    private String appId;

    @Transactional
    public RoomJoinResponse joinRoom(String roomCode) {

        User user = getAuthenticatedUser();

        Room room = roomRepository.findByRoomCode(roomCode)
                        .orElseThrow(() ->
                                new BusinessException(
                                        "Room not found"
                                )
                        );

        RoomParticipants participant =
                roomParticipantRepository
                        .findByRoomAndUser(
                                room,
                                user
                        )
                        .orElseThrow(() ->
                                new BusinessException(
                                        "Access denied"
                                )
                        );


        Integer uid =Math.abs(user.getId().hashCode());

        String token = agoraTokenService.generateToken(roomCode, uid);

        return RoomJoinResponse.builder()
                .appId(appId)
                .channelName(roomCode)
                .token(token)
                .uid(uid)
                .build();
    }


    @Transactional
    public RoleChangeResponse changeRole(String roomCode, String language) {
        User user = getAuthenticatedUser();

        Room room = roomRepository.findByRoomCodeForUpdate(roomCode).orElseThrow(
                                () -> new BusinessException("Room not found"
                                ));

        RoomParticipants currentParticipant = roomParticipantRepository.findByRoomAndUser(room, user)
                        .orElseThrow(() ->
                                new BusinessException(
                                        "User is not part of this room"
                                )
                        );
        List<RoomParticipants> participants = roomParticipantRepository.findByRoom(room);

        if (participants.size() != 2) {
            throw new BusinessException(
                    "Role change requires exactly 2 participants"
            );
        }

        participants.forEach(participant -> {
            if (
                    participant.getParticipantRole()
                            == ParticipantRole.INTERVIEWER
            ) {
                participant.setParticipantRole(
                        ParticipantRole.INTERVIEWEE
                );

            } else if (
                    participant.getParticipantRole()
                            == ParticipantRole.INTERVIEWEE
            ) {
                participant.setParticipantRole(
                        ParticipantRole.INTERVIEWER
                );
            } else {
                throw new BusinessException(
                        "Invalid participant role"
                );
            }
        });

        roomParticipantRepository.saveAll(participants);
        log.info("role of Room participants changed");

        problemAssignmentServiceImp.changeAssignProblemToRoom(room,participants.getFirst(),participants.getLast());

        AssignedProblemResponse problem = problemServiceImp.getAssignedProblem(
                        roomCode,
                        language
                );


        List<UserRoleResponse> roles =
                participants.stream()
                        .map(p ->
                                UserRoleResponse.builder()
                                        .userId(p.getUser().getId())
                                        .role(p.getParticipantRole())
                                        .build()
                        )
                        .toList();

        RoleChangeResponse response =
                RoleChangeResponse.builder()
                        .eventType("ROLE_CHANGED")
                        .roles(roles)
                        .problem(problem)
                        .build();

        messagingTemplate.convertAndSend(
                "/topic/room/" + roomCode + "/role",
                response
        );

        return response;

    }

    @Override
    @Transactional
    public JoinRoomResponse joinExistingRoom(String roomCode, String language) {


        User user =getAuthenticatedUser();

        Room room =roomRepository.findByRoomCode(roomCode).orElseThrow(() ->
                                new BusinessException("Room not found")
                        );

        RoomParticipants me = roomParticipantRepository.findByRoomAndUser(
                        room,
                        user
                ).orElseThrow(() ->
                        new BusinessException(
                                "User is not part of this room"
                        )
                );

        if (
                room.getStatus() == RoomStatus.COMPLETED ||
                        room.getStatus() == RoomStatus.EXPIRED
        ) {
            throw new BusinessException(
                    "Room is already closed"
            );
        }

        if (me.getJoinedAt() == null) {
            me.setJoinedAt(LocalDateTime.now());
            roomParticipantRepository.save(me);
        }

        long joinedCount =
                roomParticipantRepository
                        .countByRoomAndJoinedAt(room.getId());

        if (
                joinedCount >= 2 &&
                        room.getStatus() != RoomStatus.IN_PROGRESS
        ) {

            room.setStatus(RoomStatus.IN_PROGRESS);

            if (room.getStartAt() == null) {
                room.setStartAt(LocalDateTime.now());
            }

            if (room.getExpiresAt() == null) {
                room.setExpiresAt(
                        LocalDateTime.now().plusMinutes(60)
                );
            }

            roomRepository.save(room);
        }

        List<RoomParticipants> participants = roomParticipantRepository.findByRoom(room);

        RoomParticipants partner = participants.stream()
                        .filter(p ->
                                !p.getUser().getId()
                                        .equals(user.getId())
                        )
                        .findFirst()
                        .orElse(null);

        Long remainingSeconds =
                room.getExpiresAt() == null
                        ? null
                        : Math.max(
                        0,
                        Duration.between(
                                LocalDateTime.now(),
                                room.getExpiresAt()
                        ).getSeconds()
                );

        AssignedProblemResponse problem =problemServiceImp.getAssignedProblem(
                        roomCode,
                        "Java"
                );

        String agoraToken =
                agoraTokenService.generateToken(
                        room.getRoomCode(),
                        user.getId().intValue()
                );

        return JoinRoomResponse.builder()
                .roomCode(room.getRoomCode())
                .roomStatus(room.getStatus().name())
                .roomMode(room.getRoomMode().name())
                .lanuage(room.getCurrentLanguage())
                .appId(appId)
                .channelName(room.getRoomCode())
                .token(agoraToken)
                .uid(user.getId().intValue())
                .myRole(
                        me.getParticipantRole() != null
                                ? me.getParticipantRole().name()
                                : null
                )
                .partnerRole(
                        partner != null &&
                                partner.getParticipantRole() != null
                                ? partner.getParticipantRole().name()
                                : null
                )
                .topic(room.getTopic())
                .level(room.getLevel())
                .remainingSeconds(remainingSeconds)
                .problem(problem)
                .build();

    }

    @Override
    @Transactional
    public void changeLanguage(String roomCode, String language) {
        User user = getAuthenticatedUser();

            Room room =
                    roomRepository.findByRoomCode(roomCode)
                            .orElseThrow(() ->
                                    new BusinessException("Room not found")
                            );

            RoomParticipants participant =
                    roomParticipantRepository.findByRoomAndUser(room, user)
                            .orElseThrow(() ->
                                    new BusinessException("You are not part of this room")
                            );


            String normalizedLanguage =
                    normalizeLanguage(language);

            room.setCurrentLanguage(normalizedLanguage);

            roomRepository.save(room);

            messagingTemplate.convertAndSend(
                    "/topic/room/" + roomCode + "/language",
                    RoomLanguageChangedEvent.builder()
                            .eventType("LANGUAGE_CHANGED")
                            .roomCode(roomCode)
                            .language(normalizedLanguage)
                            .build()
            );
        }


    private String normalizeLanguage(String language) {

        String value =
                language.trim().toLowerCase();

        return switch (value) {
            case "java", "cpp", "python", "javascript" -> value;
            default -> throw new BusinessException(
                    "Unsupported language"
            );
        };
    }

    private User getAuthenticatedUser() {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated() ||authentication instanceof AnonymousAuthenticationToken){
            throw new BusinessException("User not authorized");
        }

        String email = authentication.getName();
        return userRepository.findByEmail(email).orElseThrow(() -> new BusinessException("User not found"));
    }

}
