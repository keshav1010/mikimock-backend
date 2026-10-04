package MikiMock.com.MikiMock.Interview.services;

import MikiMock.com.MikiMock.Interview.entity.Room;
import MikiMock.com.MikiMock.Room.repository.RoomRepository;
import MikiMock.com.MikiMock.WebSocket.MatchNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class MatchMakerScheduler {

    private static final int MAX_EXACT_MATCHES_PER_QUEUE = 100;
    private static final int MATCHING_TIMEOUT_MINUTES = 3;

    private final MatchNotificationService matchNotificationService;
    private final MatchQueueService matchQueueService;
    private final MatchCreationService matchCreationService;
    private final RoomRepository roomRepository;
    private final InterviewService interviewService;

    // ============================================================
    // MAIN SCHEDULER
    // ============================================================

    @Scheduled(fixedRate = 6000)
    public void matchUser() {

        log.debug("Queue matching cycle started");

        /*
         * --------------------------------------------------------
         * FIRST:
         * Cancel users who have been waiting for more than
         * 3 minutes.
         * --------------------------------------------------------
         */
        cancelTimedOutMatchmaking();

        Set<String> activeQueues =
                matchQueueService.getActiveQueues();

        if (activeQueues == null ||
                activeQueues.isEmpty()) {

            log.debug("No active matchmaking queues");
            return;
        }

        /*
         * --------------------------------------------------------
         * PHASE 1
         *
         * Topic + Level exact matching.
         * --------------------------------------------------------
         */
        for (String queueKey : activeQueues) {

            processExactQueue(queueKey);
        }

        /*
         * --------------------------------------------------------
         * PHASE 2
         *
         * Topic-only fallback.
         *
         * This intentionally runs AFTER exact matching.
         *
         * Therefore:
         *
         * Easy + Easy
         * -> exact first
         *
         * Medium + Medium
         * -> exact first
         *
         * remaining Easy + Medium
         * -> fallback after waiting threshold
         * --------------------------------------------------------
         */
        processTopicFallbackMatching();

        log.debug("Queue matching cycle completed");
    }

    // ============================================================
    // PHASE 1 - TOPIC + LEVEL
    // ============================================================

    private void processExactQueue(
            String queueKey
    ) {

        String queueLockToken =
                matchQueueService
                        .tryLockQueue(queueKey);

        if (queueLockToken == null) {

            log.debug(
                    "Queue {} is locked by another instance",
                    queueKey
            );

            return;
        }

        try {

            matchQueueService.cleanupQueue(
                    queueKey
            );

            QueueMeta meta =
                    matchQueueService.parseQueueKey(
                            queueKey
                    );

            int matchesCreated = 0;

            while (
                    matchesCreated
                            < MAX_EXACT_MATCHES_PER_QUEUE
            ) {

                Long size =
                        matchQueueService
                                .getQueueSizeByKey(
                                        queueKey
                                );

                if (size == null ||
                        size < 2) {

                    break;
                }

                /*
                 * ------------------------------------------------
                 * IMPORTANT:
                 *
                 * Lock USER immediately after POP.
                 *
                 * This is what prevents the fallback matcher
                 * from taking the same user at the same time.
                 * ------------------------------------------------
                 */
                Long user1Id =
                        matchQueueService
                                .popUserByKey(
                                        queueKey
                                );

                if (user1Id == null) {
                    break;
                }

                String user1Lock =
                        matchQueueService
                                .tryLockUserForMatching(
                                        user1Id
                                );

                if (user1Lock == null) {

                    /*
                     * Another matcher owns this user.
                     *
                     * Put user back without changing joinedAt.
                     */
                    matchQueueService
                            .addUserBackToFront(
                                    queueKey,
                                    user1Id
                            );

                    continue;
                }

                Long user2Id = null;
                String user2Lock = null;

                try {

                    user2Id =
                            matchQueueService
                                    .popUserByKey(
                                            queueKey
                                    );

                    if (user2Id == null) {

                        matchQueueService
                                .addUserBackToFront(
                                        queueKey,
                                        user1Id
                                );

                        break;
                    }

                    /*
                     * Same user should never normally occur,
                     * but protect against corrupted Redis state.
                     */
                    if (user1Id.equals(user2Id)) {

                        log.warn(
                                "Same user popped twice userId={}",
                                user1Id
                        );

                        matchQueueService
                                .addUserBackToFront(
                                        queueKey,
                                        user1Id
                                );

                        continue;
                    }

                    /*
                     * Acquire second user lock.
                     */
                    user2Lock =
                            matchQueueService
                                    .tryLockUserForMatching(
                                            user2Id
                                    );

                    if (user2Lock == null) {

                        /*
                         * user2 is currently being processed by
                         * another matcher.
                         *
                         * Requeue both.
                         */
                        matchQueueService
                                .addUserBackToFront(
                                        queueKey,
                                        user2Id
                                );

                        matchQueueService
                                .addUserBackToFront(
                                        queueKey,
                                        user1Id
                                );

                        continue;
                    }

                    matchQueueService
                            .markProcessing(
                                    queueKey,
                                    user1Id
                            );

                    matchQueueService
                            .markProcessing(
                                    queueKey,
                                    user2Id
                            );

                    /*
                     * ------------------------------------------------
                     * Validate active rooms.
                     * ------------------------------------------------
                     */
                    Optional<Room> user1ActiveRoom =
                            roomRepository
                                    .findActiveRoomByUserId(
                                            user1Id
                                    );

                    Optional<Room> user2ActiveRoom =
                            roomRepository
                                    .findActiveRoomByUserId(
                                            user2Id
                                    );

                    if (user1ActiveRoom.isPresent()) {

                        notifyExistingRoom(
                                user1Id,
                                user1ActiveRoom.get()
                        );

                        matchQueueService
                                .removeUserTracking(
                                        queueKey,
                                        user1Id
                                );

                        matchQueueService
                                .removeProcessing(
                                        queueKey,
                                        user2Id
                                );

                        matchQueueService
                                .addUserBackToFront(
                                        queueKey,
                                        user2Id
                                );

                        continue;
                    }

                    if (user2ActiveRoom.isPresent()) {

                        notifyExistingRoom(
                                user2Id,
                                user2ActiveRoom.get()
                        );

                        matchQueueService
                                .removeUserTracking(
                                        queueKey,
                                        user2Id
                                );

                        matchQueueService
                                .removeProcessing(
                                        queueKey,
                                        user1Id
                                );

                        matchQueueService
                                .addUserBackToFront(
                                        queueKey,
                                        user1Id
                                );

                        continue;
                    }

                    /*
                     * ------------------------------------------------
                     * EXACT MATCH
                     * ------------------------------------------------
                     */
                    Room room =
                            matchCreationService.createMatch(
                                    user1Id,
                                    user2Id,
                                    meta.topic(),
                                    meta.level()
                            );

                    matchQueueService
                            .removeUserTracking(
                                    queueKey,
                                    user1Id
                            );

                    matchQueueService
                            .removeUserTracking(
                                    queueKey,
                                    user2Id
                            );

                    notifyMatch(
                            user1Id,
                            user2Id,
                            room
                    );

                    matchesCreated++;

                } catch (Exception e) {

                    log.error(
                            "Exact match creation failed users={} and {}",
                            user1Id,
                            user2Id,
                            e
                    );

                    /*
                     * Requeue only users that were actually popped.
                     */
                    if (user2Id != null) {

                        matchQueueService
                                .removeProcessing(
                                        queueKey,
                                        user2Id
                                );

                        matchQueueService
                                .addUserBackToFront(
                                        queueKey,
                                        user2Id
                                );
                    }

                    matchQueueService
                            .removeProcessing(
                                    queueKey,
                                    user1Id
                            );

                    matchQueueService
                            .addUserBackToFront(
                                    queueKey,
                                    user1Id
                            );

                    /*
                     * Don't spin indefinitely on a broken pair.
                     */
                    break;

                } finally {

                    /*
                     * Release user2 lock first.
                     */
                    if (user2Lock != null) {

                        matchQueueService
                                .unlockUserForMatching(
                                        user2Id,
                                        user2Lock
                                );
                    }

                    /*
                     * Release user1 lock.
                     */
                    matchQueueService
                            .unlockUserForMatching(
                                    user1Id,
                                    user1Lock
                            );
                }
            }

        } finally {

            /*
             * SAFE owner-checked unlock.
             */
            matchQueueService
                    .unlockQueue(
                            queueKey,
                            queueLockToken
                    );
        }
    }

    // ============================================================
    // PHASE 2 - TOPIC ONLY FALLBACK
    // ============================================================

    private void processTopicFallbackMatching() {

        List<MatchQueueService.WaitingUser> candidates =
                matchQueueService
                        .getFallbackCandidates();

        if (candidates.isEmpty()) {
            return;
        }

        /*
         * Group by topic.
         *
         * Level is intentionally ignored here.
         */
        Map<String, List<MatchQueueService.WaitingUser>>
                candidatesByTopic =
                candidates.stream()
                        .collect(
                                java.util.stream.Collectors.groupingBy(
                                        MatchQueueService.WaitingUser::getTopic
                                )
                        );

        for (
                List<MatchQueueService.WaitingUser> topicCandidates
                : candidatesByTopic.values()
        ) {

            /*
             * Oldest first.
             */
            topicCandidates.sort(
                    Comparator.comparingLong(
                            MatchQueueService.WaitingUser::getJoinedAt
                    )
            );

            int index = 0;

            while (
                    index + 1
                            < topicCandidates.size()
            ) {

                MatchQueueService.WaitingUser candidate1 =
                        topicCandidates.get(index);

                MatchQueueService.WaitingUser candidate2 =
                        topicCandidates.get(index + 1);

                /*
                 * Try to acquire both users.
                 *
                 * If either one is already being processed by
                 * exact matching or another application instance,
                 * do NOT force the match.
                 */
                String lock1 =
                        matchQueueService
                                .tryLockUserForMatching(
                                        candidate1.getUserId()
                                );

                if (lock1 == null) {

                    index++;
                    continue;
                }

                String lock2 = null;

                try {

                    lock2 =
                            matchQueueService
                                    .tryLockUserForMatching(
                                            candidate2.getUserId()
                                    );

                    if (lock2 == null) {

                        index++;
                        continue;
                    }

                    /*
                     * ------------------------------------------------
                     * Re-check Redis state after obtaining locks.
                     *
                     * Candidate snapshot may now be stale.
                     * ------------------------------------------------
                     */
                    if (!matchQueueService
                            .isUserMemberOfQueue(
                                    candidate1.getQueueKey(),
                                    candidate1.getUserId()
                            )) {

                        index++;
                        continue;
                    }

                    if (!matchQueueService
                            .isUserMemberOfQueue(
                                    candidate2.getQueueKey(),
                                    candidate2.getUserId()
                            )) {

                        index++;
                        continue;
                    }

                    /*
                     * User may have entered processing state after
                     * our candidate snapshot.
                     */
                    if (matchQueueService.isProcessing(
                            candidate1.getQueueKey(),
                            candidate1.getUserId()
                    )) {

                        index++;
                        continue;
                    }

                    if (matchQueueService.isProcessing(
                            candidate2.getQueueKey(),
                            candidate2.getUserId()
                    )) {

                        index++;
                        continue;
                    }

                    /*
                     * Verify they are still actually old enough.
                     */
                    Long currentJoinedAt1 =
                            matchQueueService.getJoinedAt(
                                    candidate1.getQueueKey(),
                                    candidate1.getUserId()
                            );

                    Long currentJoinedAt2 =
                            matchQueueService.getJoinedAt(
                                    candidate2.getQueueKey(),
                                    candidate2.getUserId()
                            );

                    if (currentJoinedAt1 == null ||
                            currentJoinedAt2 == null) {

                        index++;
                        continue;
                    }

                    /*
                     * Mark processing in both source queues.
                     */
                    matchQueueService.markProcessing(
                            candidate1.getQueueKey(),
                            candidate1.getUserId()
                    );

                    matchQueueService.markProcessing(
                            candidate2.getQueueKey(),
                            candidate2.getUserId()
                    );

                    /*
                     * ------------------------------------------------
                     * Active room validation.
                     * ------------------------------------------------
                     */
                    Optional<Room> user1ActiveRoom =
                            roomRepository
                                    .findActiveRoomByUserId(
                                            candidate1.getUserId()
                                    );

                    Optional<Room> user2ActiveRoom =
                            roomRepository
                                    .findActiveRoomByUserId(
                                            candidate2.getUserId()
                                    );

                    if (user1ActiveRoom.isPresent()) {

                        notifyExistingRoom(
                                candidate1.getUserId(),
                                user1ActiveRoom.get()
                        );

                        matchQueueService
                                .removeUserTracking(
                                        candidate1.getQueueKey(),
                                        candidate1.getUserId()
                                );

                        matchQueueService
                                .removeProcessing(
                                        candidate2.getQueueKey(),
                                        candidate2.getUserId()
                                );

                        matchQueueService
                                .addUserBackToFront(
                                        candidate2.getQueueKey(),
                                        candidate2.getUserId()
                                );

                        index += 2;
                        continue;
                    }

                    if (user2ActiveRoom.isPresent()) {

                        notifyExistingRoom(
                                candidate2.getUserId(),
                                user2ActiveRoom.get()
                        );

                        matchQueueService
                                .removeUserTracking(
                                        candidate2.getQueueKey(),
                                        candidate2.getUserId()
                                );

                        matchQueueService
                                .removeProcessing(
                                        candidate1.getQueueKey(),
                                        candidate1.getUserId()
                                );

                        matchQueueService
                                .addUserBackToFront(
                                        candidate1.getQueueKey(),
                                        candidate1.getUserId()
                                );

                        index += 2;
                        continue;
                    }

                    /*
                     * ------------------------------------------------
                     * Remove from their original queues.
                     *
                     * Topic can be same but levels may differ.
                     * ------------------------------------------------
                     */
                    matchQueueService
                            .removeUserFromQueue(
                                    candidate1.getQueueKey(),
                                    candidate1.getUserId()
                            );

                    matchQueueService
                            .removeUserFromQueue(
                                    candidate2.getQueueKey(),
                                    candidate2.getUserId()
                            );

                    /*
                     * ------------------------------------------------
                     * IMPORTANT:
                     *
                     * Level is now MIXED because fallback ignores
                     * level.
                     * ------------------------------------------------
                     */
                    Room room =
                            matchCreationService.createMatch(
                                    candidate1.getUserId(),
                                    candidate2.getUserId(),
                                    candidate1.getTopic(),
                                    "MIXED"
                            );

                    notifyMatch(
                            candidate1.getUserId(),
                            candidate2.getUserId(),
                            room
                    );

                    log.info(
                            "Fallback topic match created users={}({}) and {}({}) topic={}",
                            candidate1.getUserId(),
                            candidate1.getLevel(),
                            candidate2.getUserId(),
                            candidate2.getLevel(),
                            candidate1.getTopic()
                    );

                    index += 2;

                } catch (Exception e) {

                    log.error(
                            "Fallback match failed users={} and {}",
                            candidate1.getUserId(),
                            candidate2.getUserId(),
                            e
                    );

                    /*
                     * If creation failed, put both users back into
                     * their ORIGINAL queues.
                     *
                     * joinedAt is preserved.
                     */
                    matchQueueService
                            .removeProcessing(
                                    candidate1.getQueueKey(),
                                    candidate1.getUserId()
                            );

                    matchQueueService
                            .removeProcessing(
                                    candidate2.getQueueKey(),
                                    candidate2.getUserId()
                            );

                    matchQueueService
                            .addUserBackToFront(
                                    candidate1.getQueueKey(),
                                    candidate1.getUserId()
                            );

                    matchQueueService
                            .addUserBackToFront(
                                    candidate2.getQueueKey(),
                                    candidate2.getUserId()
                            );

                    /*
                     * Don't retry this pair repeatedly in the same
                     * scheduler cycle.
                     */
                    index += 2;

                } finally {

                    if (lock2 != null) {

                        matchQueueService
                                .unlockUserForMatching(
                                        candidate2.getUserId(),
                                        lock2
                                );
                    }

                    matchQueueService
                            .unlockUserForMatching(
                                    candidate1.getUserId(),
                                    lock1
                            );
                }
            }
        }
    }


    // ============================================================
    // NOTIFICATIONS
    // ============================================================

    private void notifyMatch(
            Long user1Id,
            Long user2Id,
            Room room
    ) {

        matchNotificationService
                .notifyMatchFound(
                        user1Id,
                        room.getRoomCode()
                );

        matchNotificationService
                .notifyMatchFound(
                        user2Id,
                        room.getRoomCode()
                );
    }

    private void cancelTimedOutMatchmaking() {

        long deadline =
                System.currentTimeMillis()
                        - (MATCHING_TIMEOUT_MINUTES * 60_000L);

        List<MatchQueueService.WaitingUser> timedOutUsers =
                matchQueueService
                        .getUsersWaitingSince(deadline);

        if (timedOutUsers == null ||
                timedOutUsers.isEmpty()) {

            return;
        }

        log.info(
                "Found {} users whose matchmaking timeout has expired",
                timedOutUsers.size()
        );

        for (
                MatchQueueService.WaitingUser waitingUser
                : timedOutUsers
        ) {

            Long userId =
                    waitingUser.getUserId();

            String queueKey =
                    waitingUser.getQueueKey();

            String userLockToken =
                    matchQueueService
                            .tryLockUserForMatching(userId);

            /*
             * Another matcher is currently processing this user.
             */
            if (userLockToken == null) {

                continue;
            }

            try {

                /*
                 * ------------------------------------------------
                 * IMPORTANT:
                 *
                 * Candidate list can be stale because another
                 * matcher may have already matched this user.
                 *
                 * Re-check Redis membership.
                 * ------------------------------------------------
                 */
                if (!matchQueueService.isUserMemberOfQueue(
                        queueKey,
                        userId
                )) {

                    continue;
                }

                /*
                 * ------------------------------------------------
                 * Re-check processing state.
                 * ------------------------------------------------
                 */
                if (matchQueueService.isProcessing(
                        queueKey,
                        userId
                )) {

                    continue;
                }

                /*
                 * ------------------------------------------------
                 * Re-check joinedAt.
                 * ------------------------------------------------
                 */
                Long joinedAt =
                        matchQueueService.getJoinedAt(
                                queueKey,
                                userId
                        );

                if (joinedAt == null) {

                    continue;
                }

                /*
                 * User may have been requeued recently.
                 */
                if (joinedAt > deadline) {

                    continue;
                }

                /*
                 * ------------------------------------------------
                 * Mark processing before removing the user.
                 * ------------------------------------------------
                 */
                matchQueueService.markProcessing(
                        queueKey,
                        userId
                );

                /*
                 * ------------------------------------------------
                 * FINAL REMOVE
                 *
                 * This removes:
                 *
                 * 1. Redis queue membership
                 * 2. joinedAt
                 * 3. processing state
                 * 4. user membership lock
                 *
                 * Do NOT use addUserBackToFront() here.
                 * ------------------------------------------------
                 */
                matchQueueService.removeUserFromQueue(
                        queueKey,
                        userId
                );

                /*
                 * ------------------------------------------------
                 * Cancel scheduled interview.
                 * ------------------------------------------------
                 */
                cancelScheduledInterview(userId);

                /*
                 * ------------------------------------------------
                 * Notify user.
                 * ------------------------------------------------
                 */
                notifyMatchmakingTimeout(userId);

                log.info(
                        "Matchmaking cancelled after {} minutes userId={} queue={}",
                        MATCHING_TIMEOUT_MINUTES,
                        userId,
                        queueKey
                );

            } catch (Exception e) {

                log.error(
                        "Failed to process matchmaking timeout userId={} queue={}",
                        userId,
                        queueKey,
                        e
                );

            } finally {

                matchQueueService.removeProcessing(
                        queueKey,
                        userId
                );

                matchQueueService.unlockUserForMatching(
                        userId,
                        userLockToken
                );
            }
        }
    }

    private void cancelScheduledInterview(Long userId) {

        try {

            interviewService
                    .cancelInterviewForMatchmakingTimeout(
                            userId
                    );

        } catch (Exception e) {

            /*
             * Do not let a cancellation/update problem
             * break the matchmaking scheduler.
             */
            log.error(
                    "Failed to cancel scheduled interview after matchmaking timeout userId={}",
                    userId,
                    e
            );
        }
    }

    private void notifyMatchmakingTimeout(
            Long userId
    ) {

        try {

            matchNotificationService
                    .notifyMatchmakingCancelled(
                            userId,
                            "We couldn't find a suitable interview partner within the 3-minute matchmaking window. "
                                    + "Your interview has been cancelled. "
                                    + "Please try again at another scheduled time."
                    );

        } catch (Exception e) {

            /*
             * Notification failure must NEVER requeue the user.
             *
             * The matchmaking cancellation has already happened.
             */
            log.error(
                    "Failed to send matchmaking timeout notification userId={}",
                    userId,
                    e
            );
        }
    }

    private void notifyExistingRoom(
            Long userId,
            Room room
    ) {

        matchNotificationService
                .notifyMatchFound(
                        userId,
                        room.getRoomCode()
                );
    }
}






//Previous v2

//package MikiMock.com.MikiMock.Interview.services;
//
//import MikiMock.com.MikiMock.Common.Exception.BusinessException;
//import MikiMock.com.MikiMock.Interview.AgoraSessionTracking.service.AgoraSessionServiceInt;
//import MikiMock.com.MikiMock.Interview.Z_ProblemAssigner.ProblemAssignmentService;
//import MikiMock.com.MikiMock.Interview.entity.*;
//import MikiMock.com.MikiMock.Room.repository.RoomParticipantRepository;
//import MikiMock.com.MikiMock.Room.repository.RoomRepository;
//import MikiMock.com.MikiMock.User.entity.User;
//import MikiMock.com.MikiMock.User.repository.UserRepository;
//import MikiMock.com.MikiMock.WebSocket.MatchNotificationService;
//import jakarta.transaction.Transactional;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.scheduling.annotation.Scheduled;
//import org.springframework.stereotype.Component;
//
//import java.time.LocalDateTime;
//import java.util.Optional;
//import java.util.Set;
//import java.util.UUID;
//
//
//@Component
//@RequiredArgsConstructor
//@Slf4j
//public class MatchMakerScheduler {
//
//    private final MatchNotificationService matchNotificationService;
//    private final MatchQueueService matchQueueService;
//    private final UserRepository userRepository;
//    private final RoomRepository roomRepository;
//    private final RoomParticipantRepository roomParticipantRepository;
//    private final ProblemAssignmentService problemAssignmentService;
//    private final AgoraSessionServiceInt agoraSessionService;
//
//    @Scheduled(fixedRate = 6000)
//    public void matchUser() {
//
//        log.info("Queue matching is running");
//
//        Set<String> activeQueues = matchQueueService.getActiveQueues();
//
//        if (activeQueues == null || activeQueues.isEmpty()) {
//            log.info("No active matchmaking queues");
//            return;
//        }
//
//        for (String queueKey : activeQueues) {
//
//            if (!matchQueueService.tryLockQueue(queueKey)) {
//                log.info("Queue {} locked by another instance", queueKey);
//                continue;
//            }
//
//            try {
//                matchQueueService.cleanupQueue(queueKey);
//                processQueue(queueKey);
//            } finally {
//                matchQueueService.unlockQueue(queueKey);
//            }
//        }
//    }
//
//    private void processQueue(String queueKey) {
//
//        QueueMeta meta = matchQueueService.parseQueueKey(queueKey);
//
//        while (true) {
//
//            Long size = matchQueueService.getQueueSizeByKey(queueKey);
//            log.info("queueKey={} size={}", queueKey, size);
//
//            if (size == null || size < 2) {
//                break;
//            }
//
//            Long user1Id = matchQueueService.popUserByKey(queueKey);
//            Long user2Id = matchQueueService.popUserByKey(queueKey);
//
//            if (user1Id == null || user2Id == null) {
//                matchQueueService.addUserBackToFront(queueKey, user2Id);
//                matchQueueService.addUserBackToFront(queueKey, user1Id);
//                break;
//            }
//
//            if (user1Id.equals(user2Id)) {
//                log.warn("Same user popped twice userId={}", user1Id);
//                matchQueueService.addUserBackToFront(queueKey, user1Id);
//                break;
//            }
//
//            matchQueueService.markProcessing(queueKey, user1Id);
//            matchQueueService.markProcessing(queueKey, user2Id);
//
//            try {
//
//                Optional<Room> user1ActiveRoom =
//                        roomRepository.findActiveRoomByUserId(user1Id);
//
//                Optional<Room> user2ActiveRoom =
//                        roomRepository.findActiveRoomByUserId(user2Id);
//
//                if (user1ActiveRoom.isPresent()) {
//                    matchNotificationService.notifyMatchFound(
//                            user1Id,
//                            user1ActiveRoom.get().getRoomCode()
//                    );
//                    matchQueueService.removeUserTracking(queueKey, user1Id);
//                    matchQueueService.removeProcessing(queueKey, user2Id);
//                    matchQueueService.addUserBackToFront(queueKey, user2Id);
//                    continue;
//                }
//
//                if (user2ActiveRoom.isPresent()) {
//                    matchNotificationService.notifyMatchFound(
//                            user2Id,
//                            user2ActiveRoom.get().getRoomCode()
//                    );
//                    matchQueueService.removeUserTracking(queueKey, user2Id);
//                    matchQueueService.removeProcessing(queueKey, user1Id);
//                    matchQueueService.addUserBackToFront(queueKey, user1Id);
//                    continue;
//                }
//
//                Room room =
//                        createMatch(
//                                user1Id,
//                                user2Id,
//                                meta.topic(),
//                                meta.level()
//                        );
//
//                matchQueueService.removeUserTracking(queueKey, user1Id);
//                matchQueueService.removeUserTracking(queueKey, user2Id);
//
//                matchNotificationService.notifyMatchFound(
//                        user1Id,
//                        room.getRoomCode()
//                );
//
//                matchNotificationService.notifyMatchFound(
//                        user2Id,
//                        room.getRoomCode()
//                );
//
//            } catch (Exception e) {
//
//                log.error(
//                        "Match creation failed for users {} and {}",
//                        user1Id,
//                        user2Id,
//                        e
//                );
//
//                matchQueueService.removeProcessing(queueKey, user1Id);
//                matchQueueService.removeProcessing(queueKey, user2Id);
//
//                matchQueueService.addUserBackToFront(queueKey, user2Id);
//                matchQueueService.addUserBackToFront(queueKey, user1Id);
//
//                break;
//            }
//        }
//
//        Long remainingSize = matchQueueService.getQueueSizeByKey(queueKey);
//
//        if (remainingSize == null || remainingSize == 0) {
//            matchQueueService.removeActiveQueue(queueKey);
//        }
//    }
//
//    @Transactional
//    public Room createMatch(
//            Long user1Id,
//            Long user2Id,
//            String topic,
//            String level
//    ) {
//
//        User user1 =
//                userRepository.findById(user1Id)
//                        .orElseThrow(() ->
//                                new BusinessException("User not found")
//                        );
//
//        User user2 =
//                userRepository.findById(user2Id)
//                        .orElseThrow(() ->
//                                new BusinessException("User not found")
//                        );
//
//        return createRoom(user1, user2, topic, level);
//    }
//
//    private Room createRoom(
//            User user1,
//            User user2,
//            String topic,
//            String level
//    ) {
//
//        String roomCode = "room_" + UUID.randomUUID();
//
//        RoomMode roomMode = resolveRoomMode(topic);
//
//        Room room =
//                Room.builder()
//                        .roomCode(roomCode)
//                        .topic(topic)
//                        .level(level)
//                        .startAt(LocalDateTime.now())
//                        .endedAt(LocalDateTime.now().plusMinutes(60).plusSeconds(15))
//                        .expiresAt(LocalDateTime.now().plusMinutes(60).plusSeconds(15))
//                        .status(RoomStatus.ACTIVE)
//                        .roomMode(roomMode)
//                        .currentLanguage(roomMode.equals(RoomMode.CODING_INTERVIEW)?"java":"General")
//                        .build();
//
//        Room savedRoom = roomRepository.save(room);
//        agoraSessionService.startSession(room);
//
//        RoomParticipants participant1 =
//                roomParticipantRepository.save(
//                        RoomParticipants.builder()
//                                .room(savedRoom)
//                                .user(user1)
//                                .participantRole(ParticipantRole.INTERVIEWER)
//                                .build()
//                );
//
//        RoomParticipants participant2 =
//                roomParticipantRepository.save(
//                        RoomParticipants.builder()
//                                .room(savedRoom)
//                                .user(user2)
//                                .participantRole(ParticipantRole.INTERVIEWEE)
//                                .build()
//                );
//
//        problemAssignmentService.assignProblemToRoom(
//                savedRoom,
//                participant1,
//                participant2
//        );
//
//        return savedRoom;
//    }
//
//    private RoomMode resolveRoomMode(String topic) {
//
//        return switch (
//                topic.trim().toUpperCase().replace(" ", "_")
//                ) {
//            case "SYSTEM_DESIGN" -> RoomMode.DESIGN_INTERVIEW;
//            case "MANAGERIAL" -> RoomMode.MANAGERIAL_INTERVIEW;
//            case "JAVA_DEVELOPMENT" -> RoomMode.JAVA_INTERVIEW;
//            default -> RoomMode.CODING_INTERVIEW;
//        };
//    }
//}




//Oldest v1

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
