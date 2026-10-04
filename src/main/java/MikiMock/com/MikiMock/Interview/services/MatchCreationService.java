package MikiMock.com.MikiMock.Interview.services;


import MikiMock.com.MikiMock.Common.Exception.BusinessException;
import MikiMock.com.MikiMock.Interview.AgoraSessionTracking.service.AgoraSessionServiceInt;
import MikiMock.com.MikiMock.Interview.Z_ProblemAssigner.ProblemAssignmentService;
import MikiMock.com.MikiMock.Interview.entity.*;
import MikiMock.com.MikiMock.Room.repository.RoomParticipantRepository;
import MikiMock.com.MikiMock.Room.repository.RoomRepository;
import MikiMock.com.MikiMock.User.entity.User;
import MikiMock.com.MikiMock.User.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class MatchCreationService {

    private final UserRepository userRepository;
    private final RoomRepository roomRepository;
    private final RoomParticipantRepository roomParticipantRepository;
    private final ProblemAssignmentService problemAssignmentService;
    private final AgoraSessionServiceInt agoraSessionService;
    private final InterviewService interviewService;


    @Transactional
    public Room createMatch(
            Long user1Id,
            Long user2Id,
            String topic,
            String level
    ) {

        User user1 =
                userRepository.findById(user1Id)
                        .orElseThrow(() ->
                                new BusinessException(
                                        "User not found"
                                )
                        );

        User user2 =
                userRepository.findById(user2Id)
                        .orElseThrow(() ->
                                new BusinessException(
                                        "User not found"
                                )
                        );

        if (!interviewService.hasJoinedInterview(user1Id)) {
            throw new BusinessException(
                    "User is no longer eligible for matchmaking: "
                            + user1Id
            );
        }

        if (!interviewService.hasJoinedInterview(user2Id)) {
            throw new BusinessException(
                    "User is no longer eligible for matchmaking: "
                            + user2Id
            );
        }

        return createRoom(
                user1,
                user2,
                topic,
                level
        );
    }

    private Room createRoom(
            User user1,
            User user2,
            String topic,
            String level
    ) {

        String roomCode =
                "room_" + UUID.randomUUID();

        RoomMode roomMode =
                resolveRoomMode(topic);

        LocalDateTime startAt =
                LocalDateTime.now();

        LocalDateTime endAt =
                startAt
                        .plusMinutes(60)
                        .plusSeconds(15);

        Room room =
                Room.builder()
                        .roomCode(roomCode)
                        .topic(topic)
                        .level(level)
                        .startAt(startAt)
                        .endedAt(endAt)
                        .expiresAt(endAt)
                        .status(RoomStatus.ACTIVE)
                        .roomMode(roomMode)
                        .currentLanguage(
                                roomMode ==
                                        RoomMode.CODING_INTERVIEW
                                        ? "java"
                                        : "General"
                        )
                        .build();

        Room savedRoom =
                roomRepository.save(room);

        RoomParticipants participant1 =
                roomParticipantRepository.save(
                        RoomParticipants.builder()
                                .room(savedRoom)
                                .user(user1)
                                .participantRole(
                                        ParticipantRole.INTERVIEWER
                                )
                                .build()
                );

        RoomParticipants participant2 =
                roomParticipantRepository.save(
                        RoomParticipants.builder()
                                .room(savedRoom)
                                .user(user2)
                                .participantRole(
                                        ParticipantRole.INTERVIEWEE
                                )
                                .build()
                );

        problemAssignmentService.assignProblemToRoom(
                savedRoom,
                participant1,
                participant2
        );

        /*
         * Agora is an external side effect.
         *
         * We don't want:
         *
         * DB transaction fails
         * BUT Agora session already exists.
         *
         * Therefore start it after DB commit.
         */
        registerAgoraAfterCommit(savedRoom);

        return savedRoom;
    }

    private void registerAgoraAfterCommit(
            Room room
    ) {

        if (!TransactionSynchronizationManager
                .isSynchronizationActive()) {

            /*
             * Fallback for cases where this method
             * happens to execute outside a transaction.
             */
            startAgoraSafely(room);
            return;
        }

        TransactionSynchronizationManager
                .registerSynchronization(
                        new TransactionSynchronization() {

                            @Override
                            public void afterCommit() {

                                startAgoraSafely(room);
                            }
                        }
                );
    }

    private void startAgoraSafely(
            Room room
    ) {

        try {

            agoraSessionService.startSession(room);

            log.info(
                    "Agora session started room={}",
                    room.getRoomCode()
            );

        } catch (Exception e) {

            /*
             * Important:
             *
             * Room is already committed.
             * Do not throw this exception back into
             * the matchmaking transaction.
             *
             * In the next production hardening step,
             * this should be moved to an outbox/retry
             * mechanism.
             */
            log.error(
                    "Agora session creation failed room={}",
                    room.getRoomCode(),
                    e
            );
        }
    }

    private RoomMode resolveRoomMode(
            String topic
    ) {

        return switch (
                topic
                        .trim()
                        .toUpperCase()
                        .replace(" ", "_")
                ) {

            case "SYSTEM_DESIGN" ->
                    RoomMode.DESIGN_INTERVIEW;

            case "MANAGERIAL" ->
                    RoomMode.MANAGERIAL_INTERVIEW;

            case "JAVA_DEVELOPMENT" ->
                    RoomMode.JAVA_INTERVIEW;

            default ->
                    RoomMode.CODING_INTERVIEW;
        };
    }
}