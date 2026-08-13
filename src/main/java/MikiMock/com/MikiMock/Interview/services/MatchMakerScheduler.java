package MikiMock.com.MikiMock.Interview.services;

import MikiMock.com.MikiMock.Common.Exception.BusinessException;
import MikiMock.com.MikiMock.Interview.AgoraSessionTracking.service.AgoraSessionServiceInt;
import MikiMock.com.MikiMock.Interview.Z_ProblemAssigner.ProblemAssignmentService;
import MikiMock.com.MikiMock.Interview.entity.*;
import MikiMock.com.MikiMock.Room.repository.RoomParticipantRepository;
import MikiMock.com.MikiMock.Room.repository.RoomRepository;
import MikiMock.com.MikiMock.User.entity.User;
import MikiMock.com.MikiMock.User.repository.UserRepository;
import MikiMock.com.MikiMock.WebSocket.MatchNotificationService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;


@Component
@RequiredArgsConstructor
@Slf4j
public class MatchMakerScheduler {

    private final MatchNotificationService matchNotificationService;
    private final MatchQueueService matchQueueService;
    private final UserRepository userRepository;
    private final RoomRepository roomRepository;
    private final RoomParticipantRepository roomParticipantRepository;
    private final ProblemAssignmentService problemAssignmentService;
    private final AgoraSessionServiceInt agoraSessionService;

    @Scheduled(fixedRate = 6000)
    public void matchUser() {

        log.info("Queue matching is running");

        Set<String> activeQueues = matchQueueService.getActiveQueues();

        if (activeQueues == null || activeQueues.isEmpty()) {
            log.info("No active matchmaking queues");
            return;
        }

        for (String queueKey : activeQueues) {

            if (!matchQueueService.tryLockQueue(queueKey)) {
                log.info("Queue {} locked by another instance", queueKey);
                continue;
            }

            try {
                matchQueueService.cleanupQueue(queueKey);
                processQueue(queueKey);
            } finally {
                matchQueueService.unlockQueue(queueKey);
            }
        }
    }

    private void processQueue(String queueKey) {

        QueueMeta meta = matchQueueService.parseQueueKey(queueKey);

        while (true) {

            Long size = matchQueueService.getQueueSizeByKey(queueKey);
            log.info("queueKey={} size={}", queueKey, size);

            if (size == null || size < 2) {
                break;
            }

            Long user1Id = matchQueueService.popUserByKey(queueKey);
            Long user2Id = matchQueueService.popUserByKey(queueKey);

            if (user1Id == null || user2Id == null) {
                matchQueueService.addUserBackToFront(queueKey, user2Id);
                matchQueueService.addUserBackToFront(queueKey, user1Id);
                break;
            }

            if (user1Id.equals(user2Id)) {
                log.warn("Same user popped twice userId={}", user1Id);
                matchQueueService.addUserBackToFront(queueKey, user1Id);
                break;
            }

            matchQueueService.markProcessing(queueKey, user1Id);
            matchQueueService.markProcessing(queueKey, user2Id);

            try {

                Optional<Room> user1ActiveRoom =
                        roomRepository.findActiveRoomByUserId(user1Id);

                Optional<Room> user2ActiveRoom =
                        roomRepository.findActiveRoomByUserId(user2Id);

                if (user1ActiveRoom.isPresent()) {
                    matchNotificationService.notifyMatchFound(
                            user1Id,
                            user1ActiveRoom.get().getRoomCode()
                    );
                    matchQueueService.removeUserTracking(queueKey, user1Id);
                    matchQueueService.removeProcessing(queueKey, user2Id);
                    matchQueueService.addUserBackToFront(queueKey, user2Id);
                    continue;
                }

                if (user2ActiveRoom.isPresent()) {
                    matchNotificationService.notifyMatchFound(
                            user2Id,
                            user2ActiveRoom.get().getRoomCode()
                    );
                    matchQueueService.removeUserTracking(queueKey, user2Id);
                    matchQueueService.removeProcessing(queueKey, user1Id);
                    matchQueueService.addUserBackToFront(queueKey, user1Id);
                    continue;
                }

                Room room =
                        createMatch(
                                user1Id,
                                user2Id,
                                meta.topic(),
                                meta.level()
                        );

                matchQueueService.removeUserTracking(queueKey, user1Id);
                matchQueueService.removeUserTracking(queueKey, user2Id);

                matchNotificationService.notifyMatchFound(
                        user1Id,
                        room.getRoomCode()
                );

                matchNotificationService.notifyMatchFound(
                        user2Id,
                        room.getRoomCode()
                );

            } catch (Exception e) {

                log.error(
                        "Match creation failed for users {} and {}",
                        user1Id,
                        user2Id,
                        e
                );

                matchQueueService.removeProcessing(queueKey, user1Id);
                matchQueueService.removeProcessing(queueKey, user2Id);

                matchQueueService.addUserBackToFront(queueKey, user2Id);
                matchQueueService.addUserBackToFront(queueKey, user1Id);

                break;
            }
        }

        Long remainingSize = matchQueueService.getQueueSizeByKey(queueKey);

        if (remainingSize == null || remainingSize == 0) {
            matchQueueService.removeActiveQueue(queueKey);
        }
    }

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
                                new BusinessException("User not found")
                        );

        User user2 =
                userRepository.findById(user2Id)
                        .orElseThrow(() ->
                                new BusinessException("User not found")
                        );

        return createRoom(user1, user2, topic, level);
    }

    private Room createRoom(
            User user1,
            User user2,
            String topic,
            String level
    ) {

        String roomCode = "room_" + UUID.randomUUID();

        RoomMode roomMode = resolveRoomMode(topic);

        Room room =
                Room.builder()
                        .roomCode(roomCode)
                        .topic(topic)
                        .level(level)
                        .startAt(LocalDateTime.now())
                        .endedAt(LocalDateTime.now().plusMinutes(60).plusSeconds(15))
                        .expiresAt(LocalDateTime.now().plusMinutes(60).plusSeconds(15))
                        .status(RoomStatus.ACTIVE)
                        .roomMode(roomMode)
                        .currentLanguage(roomMode.equals(RoomMode.CODING_INTERVIEW)?"java":"General")
                        .build();

        Room savedRoom = roomRepository.save(room);
        agoraSessionService.startSession(room);

        RoomParticipants participant1 =
                roomParticipantRepository.save(
                        RoomParticipants.builder()
                                .room(savedRoom)
                                .user(user1)
                                .participantRole(ParticipantRole.INTERVIEWER)
                                .build()
                );

        RoomParticipants participant2 =
                roomParticipantRepository.save(
                        RoomParticipants.builder()
                                .room(savedRoom)
                                .user(user2)
                                .participantRole(ParticipantRole.INTERVIEWEE)
                                .build()
                );

        problemAssignmentService.assignProblemToRoom(
                savedRoom,
                participant1,
                participant2
        );

        return savedRoom;
    }

    private RoomMode resolveRoomMode(String topic) {

        return switch (
                topic.trim().toUpperCase().replace(" ", "_")
                ) {
            case "SYSTEM_DESIGN" -> RoomMode.DESIGN_INTERVIEW;
            case "MANAGERIAL" -> RoomMode.MANAGERIAL_INTERVIEW;
            case "JAVA_DEVELOPMENT" -> RoomMode.JAVA_INTERVIEW;
            default -> RoomMode.CODING_INTERVIEW;
        };
    }
}



//@Component
//@RequiredArgsConstructor
//@Slf4j
//public class MatchMakerScheduler {
//    private final MatchNotificationService matchNotificationService;
//    private final MatchQueueService matchQueueService;
//    private final UserRepository userRepository;
//    private final RoomRepository roomRepository;
//    private final RoomParticipantRepository roomParticipantRepository;
//    private final ProblemAssignmentService problemAssignmentService;
//
//
//    @Scheduled(fixedRate = 15000)
//    public void matchUser(){
//        log.info("Queue matching is running");
//
//        Set<String> activeQueues = matchQueueService.getActiveQueues();
//
//
//        log.info("redisQueueSize = {}",activeQueues);
//
//        if(activeQueues == null || activeQueues.isEmpty()){
//            log.info("No active matchmaking queues");
//            return;
//        }
//
//        for (String queueKey : activeQueues) {
//            if (!matchQueueService.tryLockQueue(queueKey)) {
//                log.info(
//                        "Queue {} is already locked by another scheduler",
//                        queueKey
//                );
//                continue;
//            }
//
//            try {
//
//                processQueue(queueKey);
//
//            } finally {
//
//                matchQueueService.unlockQueue(queueKey);
//            }
//        }
//
//    }
//
//
//
//    private void processQueue(String queueKey) {
//        QueueMeta meta = matchQueueService.parseQueueKey(queueKey);
//
//        while(true) {
//            Long size = matchQueueService.getQueueSizeByKey(queueKey);
//            log.info("queueKey={} size={}", queueKey, size);
//
//            if (size == null || size < 2) {
//                break;
//            }
//
//
//            Long user1Id = matchQueueService.popUserByKey(queueKey );
//
//            Long user2Id = matchQueueService.popUserByKey(queueKey);
//
//
//            if (user1Id == null || user2Id == null) {
//                if(user1Id == null )matchQueueService.addUserBackToFront(queueKey, user1Id);
//                else matchQueueService.addUserBackToFront(queueKey, user2Id);
//
//                break;
//            }
//            if (user1Id.equals(user2Id)) {
//                log.warn("Same user popped twice from queue. userId={}", user1Id);
//
//                matchQueueService.addUserBackToFront(queueKey, user1Id);
//
//                break;
//            }
//
//            try {
//                createMatch(user1Id, user2Id, meta.topic(), meta.level());
//
//                matchQueueService.removeUserTracking(queueKey, user1Id);
//                matchQueueService.removeUserTracking(queueKey, user2Id);
//
//            } catch (Exception e) {
//                log.error("Match creation failed for users {} and {}", user1Id, user2Id, e);
//
//                matchQueueService.addUserBackToFront(queueKey, user2Id);
//                matchQueueService.addUserBackToFront(queueKey, user1Id);
//            }
//
//        }
//
//        Long remainingSize = matchQueueService.getQueueSizeByKey(queueKey);
//        if (remainingSize == null || remainingSize == 0) {
//            matchQueueService.removeActiveQueue(queueKey);
//        }
//    }
//
//
//    private void createMatch(Long user1Id, Long user2Id, String topic, String level) {
//
//
//        log.info("user1Id={} user2Id={} topic={} level={}", user1Id, user2Id, topic, level);
//
//        User user1 = userRepository.findById(user1Id).orElseThrow(() -> new BusinessException("User not found"));
//
//        User user2 =  userRepository.findById(user2Id).orElseThrow(() -> new BusinessException("User not found"));
//
//
//
//        Room room = createRoom(user1, user2, topic, level);
//
//        log.info("Match found user1={} user2={} roomCode={}", user1.getId(), user2.getId(), room.getRoomCode());
//
//        matchNotificationService.notifyMatchFound(
//                user1.getId(),
//                room.getRoomCode()
//        );
//
//        matchNotificationService.notifyMatchFound(
//                user2.getId(),
//                room.getRoomCode()
//        );
//    }
//
//
//
//    private Room createRoom(User user1, User user2, String topic, String level) {
//
//        log.info("In createRoom method");
//
//        String channelName = "room_" + UUID.randomUUID();
//
//        RoomMode roomMode = resolveRoomMode(topic);
//
//
//
//        Room room = Room.builder()
//                        .roomCode(channelName)
//                        .topic(topic)
//                        .level(level)
//                        .startAt(LocalDateTime.now())
//                        .endedAt(LocalDateTime.now().plusMinutes(60))
//                        .status(RoomStatus.ACTIVE)
//                        .roomMode(roomMode)
//                        .build();
//
//
//        Room savedRoom;
//
//        try {
//            savedRoom = roomRepository.save(room);
//        }
//        catch (Exception e){
//            throw new BusinessException("Room not created");
//        }
//
//        ParticipantRole user1Role = ParticipantRole.INTERVIEWER;
//
//        ParticipantRole user2Role = ParticipantRole.INTERVIEWEE;
//
//        RoomParticipants participant1 = roomParticipantRepository.save(
//                RoomParticipants.builder()
//                        .room(savedRoom)
//                        .user(user1)
//                        .participantRole(user1Role)
//                        .build()
//        );
//
//
//
//        RoomParticipants participant2 = roomParticipantRepository.save(
//                RoomParticipants.builder()
//                        .room(savedRoom)
//                        .user(user2)
//                        .participantRole(user2Role)
//                        .build()
//        );
//
//
//        try{
//            problemAssignmentService.assignProblemToRoom(savedRoom, participant1, participant2);
//        }
//        catch (Exception e){
//            throw new BusinessException("Problem is not assigned to room");
//        }
//
//
//        return savedRoom;
//    }
//
//
//
//    private RoomMode resolveRoomMode(String topic) {
//
//        return switch (
//                topic.trim()
//                        .toUpperCase()
//                        .replace(" ", "_")
//                ) {
//
//            case "DSA" -> RoomMode.CODING_INTERVIEW;
//
//            case "SYSTEM_DESIGN" -> RoomMode.DESIGN_INTERVIEW;
//
//            case "MANAGERIAL" -> RoomMode.MANAGERIAL_INTERVIEW;
//
//            case "JAVA_DEVELOPMENT" -> RoomMode.JAVA_INTERVIEW;
//
//            default -> RoomMode.CODING_INTERVIEW;
//        };
//    }
//}
