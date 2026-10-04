package MikiMock.com.MikiMock.Interview.services;

import MikiMock.com.MikiMock.Common.Exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MatchQueueService {

    private static final String ACTIVE_QUEUE_KEY = "queue:active";

    /*
     * User is considered stale only after 5 minutes.
     */
    private static final long USER_STALE_MINUTES = 5;

    /*
     * Fallback matching starts after this amount of waiting.
     *
     * Example:
     * Easy user waits 30+ seconds
     * Medium user waits 30+ seconds
     * Same topic
     * -> fallback can match them.
     */
    private static final long FALLBACK_WAIT_SECONDS = 30;

    /*
     * Queue lock protects processing of one exact queue
     * by multiple application instances.
     */
    private static final Duration QUEUE_LOCK_TTL =
            Duration.ofSeconds(60);

    /*
     * Per-user matching lock protects a user from:
     *
     * exact matcher <-> fallback matcher
     *
     * racing with each other.
     */
    private static final Duration USER_MATCHING_LOCK_TTL =
            Duration.ofSeconds(30);

    private final RedisTemplate<String, Long> redisTemplate;
    private final StringRedisTemplate stringRedisTemplate;

    /*
     * Redis Lua script:
     *
     * Delete the lock ONLY if the value is still our token.
     *
     * This prevents:
     *
     * Instance A lock expires
     * Instance B acquires same lock
     * Instance A blindly DELs it
     *
     * which would incorrectly delete B's lock.
     */
    private static final DefaultRedisScript<Long> UNLOCK_SCRIPT =
            new DefaultRedisScript<>(
                    """
                    if redis.call('get', KEYS[1]) == ARGV[1] then
                        return redis.call('del', KEYS[1])
                    else
                        return 0
                    end
                    """,
                    Long.class
            );

    private String queueKey(String topic, String level) {

        return "queue:"
                + normalize(topic)
                + ":"
                + normalize(level);
    }

    private String memberKey(String queueKey) {
        return queueKey + ":members";
    }

    private String joinedAtKey(String queueKey) {
        return queueKey + ":joined-at";
    }

    private String processingKey(String queueKey) {
        return queueKey + ":processing";
    }

    private String userLockKey(Long userId) {
        return "queue:user-lock:" + userId;
    }

    private String matchingLockKey(Long userId) {
        return "queue:matching-lock:" + userId;
    }

    private String queueLockKey(String queueKey) {
        return "lock:" + queueKey;
    }

    private String normalize(String value) {

        if (value == null || value.isBlank()) {
            throw new BusinessException("Topic/level cannot be empty");
        }

        return value
                .trim()
                .toUpperCase()
                .replace(" ", "_");
    }

    // ============================================================
    // ADD USER
    // ============================================================

    public void addUserToQueue(
            String topic,
            String level,
            Long userId
    ) {

        String key = queueKey(topic, level);

        /*
         * Existing membership lock.
         *
         * This prevents the same user from joining matchmaking
         * repeatedly.
         */
        Boolean locked =
                stringRedisTemplate.opsForValue()
                        .setIfAbsent(
                                userLockKey(userId),
                                key,
                                Duration.ofMinutes(USER_STALE_MINUTES)
                        );

        if (!Boolean.TRUE.equals(locked)) {

            log.info(
                    "User {} already exists in matchmaking",
                    userId
            );

            return;
        }

        /*
         * Remove any duplicate list occurrence before inserting.
         */
        redisTemplate.opsForList()
                .remove(key, 0, userId);

        /*
         * FIFO queue.
         */
        redisTemplate.opsForList()
                .rightPush(key, userId);

        String userValue = String.valueOf(userId);

        /*
         * Membership tracking.
         */
        stringRedisTemplate.opsForSet()
                .add(
                        memberKey(key),
                        userValue
                );

        /*
         * IMPORTANT:
         * joinedAt is created only on initial enqueue.
         */
        stringRedisTemplate.opsForHash()
                .put(
                        joinedAtKey(key),
                        userValue,
                        String.valueOf(
                                System.currentTimeMillis()
                        )
                );

        /*
         * Mark queue active.
         */
        stringRedisTemplate.opsForSet()
                .add(
                        ACTIVE_QUEUE_KEY,
                        key
                );

        log.info(
                "User {} added to matchmaking queue {}",
                userId,
                key
        );
    }

    // ============================================================
    // ACTIVE QUEUES
    // ============================================================

    public Set<String> getActiveQueues() {

        return stringRedisTemplate
                .opsForSet()
                .members(ACTIVE_QUEUE_KEY);
    }

    // ============================================================
    // QUEUE SIZE
    // ============================================================

    public Long getQueueSizeByKey(String key) {

        Long listSize =
                redisTemplate
                        .opsForList()
                        .size(key);

        Long memberSize =
                stringRedisTemplate
                        .opsForSet()
                        .size(memberKey(key));

        Long processingSize =
                stringRedisTemplate
                        .opsForSet()
                        .size(processingKey(key));

        log.debug(
                "queue={} | listSize={} | memberSize={} | processingSize={}",
                key,
                listSize,
                memberSize,
                processingSize
        );

        return listSize;
    }

    // ============================================================
    // POP
    // ============================================================

    public Long popUserByKey(String key) {

        return redisTemplate
                .opsForList()
                .leftPop(key);
    }

    // ============================================================
    // REQUEUE
    // ============================================================

    public void addUserBackToFront(
            String key,
            Long userId
    ) {

        if (userId == null) {
            return;
        }

        /*
         * Remove duplicate occurrence first.
         */
        redisTemplate.opsForList()
                .remove(key, 0, userId);

        /*
         * Put user back at front.
         */
        redisTemplate.opsForList()
                .leftPush(key, userId);

        String userValue = String.valueOf(userId);

        /*
         * Restore membership.
         */
        stringRedisTemplate.opsForSet()
                .add(
                        memberKey(key),
                        userValue
                );

        /*
         * VERY IMPORTANT:
         *
         * DO NOT reset joinedAt here.
         *
         * Otherwise a user that waited 29 seconds,
         * failed matching, and was requeued would become
         * a fresh user and could wait forever without
         * reaching fallback.
         */
        Object existingJoinedAt =
                stringRedisTemplate.opsForHash()
                        .get(
                                joinedAtKey(key),
                                userValue
                        );

        if (existingJoinedAt == null) {

            stringRedisTemplate.opsForHash()
                    .put(
                            joinedAtKey(key),
                            userValue,
                            String.valueOf(
                                    System.currentTimeMillis()
                            )
                    );
        }

        /*
         * Ensure queue remains active.
         */
        stringRedisTemplate.opsForSet()
                .add(
                        ACTIVE_QUEUE_KEY,
                        key
                );
    }

    // ============================================================
    // PROCESSING TRACKING
    // ============================================================

    public void markProcessing(
            String queueKey,
            Long userId
    ) {

        if (userId == null) {
            return;
        }

        stringRedisTemplate.opsForSet()
                .add(
                        processingKey(queueKey),
                        String.valueOf(userId)
                );
    }

    public void removeProcessing(
            String queueKey,
            Long userId
    ) {

        if (userId == null) {
            return;
        }

        stringRedisTemplate.opsForSet()
                .remove(
                        processingKey(queueKey),
                        String.valueOf(userId)
                );
    }

    public boolean isProcessing(
            String queueKey,
            Long userId
    ) {

        if (userId == null) {
            return false;
        }

        return Boolean.TRUE.equals(
                stringRedisTemplate.opsForSet()
                        .isMember(
                                processingKey(queueKey),
                                String.valueOf(userId)
                        )
        );
    }

    // ============================================================
    // USER TRACKING
    // ============================================================

    public void removeUserTracking(
            String queueKey,
            Long userId
    ) {

        if (userId == null) {
            return;
        }

        String userValue =
                String.valueOf(userId);

        /*
         * Remove queue membership.
         */
        stringRedisTemplate.opsForSet()
                .remove(
                        memberKey(queueKey),
                        userValue
                );

        /*
         * Remove waiting timestamp.
         */
        stringRedisTemplate.opsForHash()
                .delete(
                        joinedAtKey(queueKey),
                        userValue
                );

        /*
         * Remove processing state.
         */
        stringRedisTemplate.opsForSet()
                .remove(
                        processingKey(queueKey),
                        userValue
                );

        /*
         * Remove normal matchmaking membership lock.
         *
         * Matching lock is DIFFERENT and is handled
         * separately by unlockUserForMatching().
         */
        stringRedisTemplate.delete(
                userLockKey(userId)
        );
    }

    // ============================================================
    // SAFE QUEUE DISTRIBUTED LOCK
    // ============================================================

    /*
     * Returns a unique lock token if lock was acquired.
     * Returns null if another instance owns the lock.
     */
    public String tryLockQueue(String queueKey) {

        String token =
                UUID.randomUUID().toString();

        Boolean locked =
                stringRedisTemplate.opsForValue()
                        .setIfAbsent(
                                queueLockKey(queueKey),
                                token,
                                QUEUE_LOCK_TTL
                        );

        if (Boolean.TRUE.equals(locked)) {
            return token;
        }

        return null;
    }

    /*
     * Releases ONLY our own lock.
     */
    public void unlockQueue(
            String queueKey,
            String token
    ) {

        if (token == null) {
            return;
        }

        stringRedisTemplate.execute(
                UNLOCK_SCRIPT,
                List.of(queueLockKey(queueKey)),
                token
        );
    }

    // ============================================================
    // SAFE PER-USER MATCHING LOCK
    // ============================================================

    /*
     * This lock is NOT the same as userLockKey().
     *
     * userLockKey():
     *     prevents duplicate queue registration.
     *
     * matchingLockKey():
     *     prevents exact matcher and fallback matcher
     *     from matching the same user simultaneously.
     */
    public String tryLockUserForMatching(
            Long userId
    ) {

        if (userId == null) {
            return null;
        }

        String token =
                UUID.randomUUID().toString();

        Boolean locked =
                stringRedisTemplate.opsForValue()
                        .setIfAbsent(
                                matchingLockKey(userId),
                                token,
                                USER_MATCHING_LOCK_TTL
                        );

        if (Boolean.TRUE.equals(locked)) {
            return token;
        }

        return null;
    }

    public void unlockUserForMatching(
            Long userId,
            String token
    ) {

        if (userId == null || token == null) {
            return;
        }

        stringRedisTemplate.execute(
                UNLOCK_SCRIPT,
                List.of(matchingLockKey(userId)),
                token
        );
    }

    // ============================================================
    // VERIFY USER STILL BELONGS TO QUEUE
    // ============================================================

    public boolean isUserMemberOfQueue(
            String queueKey,
            Long userId
    ) {

        if (userId == null) {
            return false;
        }

        return Boolean.TRUE.equals(
                stringRedisTemplate.opsForSet()
                        .isMember(
                                memberKey(queueKey),
                                String.valueOf(userId)
                        )
        );
    }

    // ============================================================
    // JOINED AT
    // ============================================================

    public Long getJoinedAt(
            String queueKey,
            Long userId
    ) {

        Object value =
                stringRedisTemplate.opsForHash()
                        .get(
                                joinedAtKey(queueKey),
                                String.valueOf(userId)
                        );

        if (value == null) {
            return null;
        }

        try {
            return Long.parseLong(
                    value.toString()
            );
        } catch (NumberFormatException e) {

            log.warn(
                    "Invalid joinedAt for queue={} user={}",
                    queueKey,
                    userId
            );

            return null;
        }
    }

    // ============================================================
    // FALLBACK CANDIDATES
    // ============================================================

    public List<WaitingUser> getFallbackCandidates() {

        Set<String> activeQueues =
                getActiveQueues();

        if (activeQueues == null ||
                activeQueues.isEmpty()) {

            return List.of();
        }

        long fallbackBefore =
                System.currentTimeMillis()
                        - (FALLBACK_WAIT_SECONDS * 1000);

        List<WaitingUser> candidates =
                new ArrayList<>();

        for (String queueKey : activeQueues) {

            QueueMeta meta =
                    parseQueueKey(queueKey);

            Set<String> members =
                    stringRedisTemplate.opsForSet()
                            .members(
                                    memberKey(queueKey)
                            );

            if (members == null ||
                    members.isEmpty()) {

                continue;
            }

            for (String userValue : members) {

                Long userId;

                try {
                    userId =
                            Long.valueOf(userValue);
                } catch (NumberFormatException e) {

                    log.warn(
                            "Invalid user id {} in queue {}",
                            userValue,
                            queueKey
                    );

                    continue;
                }

                /*
                 * Do not consider a user already being
                 * processed by exact matching.
                 */
                if (isProcessing(queueKey, userId)) {
                    continue;
                }

                Long joinedAt =
                        getJoinedAt(
                                queueKey,
                                userId
                        );

                if (joinedAt == null) {
                    continue;
                }

                /*
                 * User must have waited at least
                 * FALLBACK_WAIT_SECONDS.
                 */
                if (joinedAt > fallbackBefore) {
                    continue;
                }

                candidates.add(
                        new WaitingUser(
                                userId,
                                queueKey,
                                meta.topic(),
                                meta.level(),
                                joinedAt
                        )
                );
            }
        }

        /*
         * Oldest users first.
         */
        candidates.sort(
                Comparator.comparingLong(
                        WaitingUser::getJoinedAt
                )
        );

        return candidates;
    }

    // ============================================================
    // CLEANUP
    // ============================================================

    public void cleanupQueue(
            String queueKey
    ) {

        Set<String> members =
                stringRedisTemplate.opsForSet()
                        .members(
                                memberKey(queueKey)
                        );

        if (members == null ||
                members.isEmpty()) {

            removeActiveQueue(queueKey);
            return;
        }

        long staleBefore =
                System.currentTimeMillis()
                        - USER_STALE_MINUTES * 60 * 1000;

        for (String userIdValue : members) {

            Long userId;

            try {
                userId =
                        Long.valueOf(userIdValue);
            } catch (NumberFormatException e) {

                stringRedisTemplate.opsForSet()
                        .remove(
                                memberKey(queueKey),
                                userIdValue
                        );

                stringRedisTemplate.opsForHash()
                        .delete(
                                joinedAtKey(queueKey),
                                userIdValue
                        );

                continue;
            }

            /*
             * Never cleanup a user currently being matched.
             */
            if (isProcessing(queueKey, userId)) {
                continue;
            }

            Object joinedAtValue =
                    stringRedisTemplate.opsForHash()
                            .get(
                                    joinedAtKey(queueKey),
                                    userIdValue
                            );

            if (joinedAtValue == null) {

                redisTemplate.opsForList()
                        .remove(
                                queueKey,
                                0,
                                userId
                        );

                removeUserTracking(
                        queueKey,
                        userId
                );

                continue;
            }

            long joinedAt;

            try {
                joinedAt =
                        Long.parseLong(
                                joinedAtValue.toString()
                        );
            } catch (NumberFormatException e) {

                redisTemplate.opsForList()
                        .remove(
                                queueKey,
                                0,
                                userId
                        );

                removeUserTracking(
                        queueKey,
                        userId
                );

                continue;
            }

            if (joinedAt < staleBefore) {

                log.info(
                        "Removing stale matchmaking user={} queue={}",
                        userId,
                        queueKey
                );

                redisTemplate.opsForList()
                        .remove(
                                queueKey,
                                0,
                                userId
                        );

                removeUserTracking(
                        queueKey,
                        userId
                );
            }
        }

        Long size =
                redisTemplate.opsForList()
                        .size(queueKey);

        if (size == null ||
                size == 0) {

            removeActiveQueue(queueKey);
        }
    }

    // ============================================================
    // ACTIVE QUEUE CLEANUP
    // ============================================================

    public void removeActiveQueue(
            String key
    ) {

        stringRedisTemplate.opsForSet()
                .remove(
                        ACTIVE_QUEUE_KEY,
                        key
                );
    }

    // ============================================================
    // QUEUE KEY PARSING
    // ============================================================

    public QueueMeta parseQueueKey(
            String key
    ) {

        String[] parts =
                key.split(":");

        if (parts.length != 3) {

            throw new BusinessException(
                    "Invalid queue key: " + key
            );
        }

        return new QueueMeta(
                parts[1],
                parts[2]
        );
    }

    // ============================================================
    // WAITING USER DTO
    // ============================================================

    @lombok.Getter
    public static class WaitingUser {

        private final Long userId;
        private final String queueKey;
        private final String topic;
        private final String level;
        private final long joinedAt;

        public WaitingUser(
                Long userId,
                String queueKey,
                String topic,
                String level,
                long joinedAt
        ) {

            this.userId = userId;
            this.queueKey = queueKey;
            this.topic = topic;
            this.level = level;
            this.joinedAt = joinedAt;
        }
    }

    public void removeUserFromQueue(
            String queueKey,
            Long userId
    ) {

        if (userId == null) {
            return;
        }

        String userValue =
                String.valueOf(userId);

        /*
         * Remove from Redis FIFO list.
         */
        redisTemplate.opsForList()
                .remove(
                        queueKey,
                        0,
                        userId
                );

        /*
         * Remove membership.
         */
        stringRedisTemplate.opsForSet()
                .remove(
                        memberKey(queueKey),
                        userValue
                );

        /*
         * Remove waiting timestamp.
         */
        stringRedisTemplate.opsForHash()
                .delete(
                        joinedAtKey(queueKey),
                        userValue
                );

        /*
         * Remove processing state.
         */
        stringRedisTemplate.opsForSet()
                .remove(
                        processingKey(queueKey),
                        userValue
                );

        /*
         * Remove duplicate-queue protection lock.
         */
        stringRedisTemplate.delete(
                userLockKey(userId)
        );

        /*
         * If queue became empty, remove it from active queues.
         */
        Long size =
                redisTemplate.opsForList()
                        .size(queueKey);

        if (size == null || size == 0) {

            removeActiveQueue(queueKey);
        }
    }

    public List<WaitingUser> getUsersWaitingSince(
            long deadline
    ) {

        List<WaitingUser> result =
                new ArrayList<>();

        Set<String> activeQueues =
                getActiveQueues();

        if (activeQueues == null ||
                activeQueues.isEmpty()) {

            return result;
        }

        for (String queueKey : activeQueues) {

            Map<Object, Object> joinedAtMap =
                    redisTemplate.opsForHash()
                            .entries(
                                    joinedAtKey(queueKey)
                            );

            if (joinedAtMap == null ||
                    joinedAtMap.isEmpty()) {

                continue;
            }

            QueueMeta meta =
                    parseQueueKey(queueKey);

            for (Map.Entry<Object, Object> entry
                    : joinedAtMap.entrySet()) {

                Long userId;

                try {

                    userId =
                            Long.valueOf(
                                    entry.getKey().toString()
                            );

                } catch (NumberFormatException e) {

                    log.warn(
                            "Invalid userId in joinedAt hash queue={} value={}",
                            queueKey,
                            entry.getKey()
                    );

                    continue;
                }

                Long joinedAt;

                try {

                    joinedAt =
                            Long.valueOf(
                                    entry.getValue().toString()
                            );

                } catch (NumberFormatException e) {

                    log.warn(
                            "Invalid joinedAt queue={} userId={}",
                            queueKey,
                            userId
                    );


                    continue;
                }

                if (joinedAt <= deadline) {

                    WaitingUser waitingUser =
                            new WaitingUser(
                                    userId,
                                    queueKey,
                                    meta.topic(),
                                    meta.level(),
                                    joinedAt
                            );

                    result.add(waitingUser);
                }
            }
        }

        return result;
    }
}












//Previous v.2

//package MikiMock.com.MikiMock.Interview.services;
//
//
//import MikiMock.com.MikiMock.Common.Exception.BusinessException;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.data.redis.core.RedisTemplate;
//import org.springframework.data.redis.core.StringRedisTemplate;
//import org.springframework.stereotype.Service;
//
//import java.time.Duration;
//import java.util.List;
//import java.util.Set;
//import java.util.stream.Collectors;
//
//
//
//@Service
//@RequiredArgsConstructor
//@Slf4j
//public class MatchQueueService {
//
//    private static final String ACTIVE_QUEUE_KEY = "queue:active";
//    private static final long USER_STALE_MINUTES = 5;
//
//    private final RedisTemplate<String, Long> redisTemplate;
//    private final StringRedisTemplate stringRedisTemplate;
//
//    private String queueKey(String topic, String level) {
//        return "queue:" + normalize(topic) + ":" + normalize(level);
//    }
//
//    private String memberKey(String queueKey) {
//        return queueKey + ":members";
//    }
//
//    private String joinedAtKey(String queueKey) {
//        return queueKey + ":joined-at";
//    }
//
//    private String processingKey(String queueKey) {
//        return queueKey + ":processing";
//    }
//
//    private String userLockKey(Long userId) {
//        return "queue:user-lock:" + userId;
//    }
//
//    private String queueLockKey(String queueKey) {
//        return "lock:" + queueKey;
//    }
//
//    private String normalize(String value) {
//        return value.trim().toUpperCase().replace(" ", "_");
//    }
//
//    public void addUserToQueue(String topic, String level, Long userId) {
//
//        String key = queueKey(topic, level);
//
//        Boolean locked =
//                stringRedisTemplate.opsForValue()
//                        .setIfAbsent(
//                                userLockKey(userId),
//                                key,
//                                Duration.ofMinutes(USER_STALE_MINUTES)
//                        );
//
//        if (!Boolean.TRUE.equals(locked)) {
//            log.info("User {} already exists in matchmaking", userId);
//            return;
//        }
//
//        redisTemplate.opsForList().remove(key, 0, userId);
//        redisTemplate.opsForList().rightPush(key, userId);
//
//        String userValue = String.valueOf(userId);
//
//        stringRedisTemplate.opsForSet().add(memberKey(key), userValue);
//        stringRedisTemplate.opsForHash().put(
//                joinedAtKey(key),
//                userValue,
//                String.valueOf(System.currentTimeMillis())
//        );
//
//        stringRedisTemplate.opsForSet().add(ACTIVE_QUEUE_KEY, key);
//    }
//
//    public Set<String> getActiveQueues() {
//        return stringRedisTemplate.opsForSet().members(ACTIVE_QUEUE_KEY);
//    }
//
//    public Long getQueueSizeByKey(String key) {
//
//        Long listSize = redisTemplate.opsForList().size(key);
//        Long memberSize = stringRedisTemplate.opsForSet().size(memberKey(key));
//        Long processingSize = stringRedisTemplate.opsForSet().size(processingKey(key));
//
//        log.info(
//                "queue={} | listSize={} | memberSize={} | processingSize={}",
//                key,
//                listSize,
//                memberSize,
//                processingSize
//        );
//
//        return listSize;
//    }
//
//    public Long popUserByKey(String key) {
//        return redisTemplate.opsForList().leftPop(key);
//    }
//
//    public void addUserBackToFront(String key, Long userId) {
//
//        if (userId == null) return;
//
//        redisTemplate.opsForList().remove(key, 0, userId);
//        redisTemplate.opsForList().leftPush(key, userId);
//
//        String userValue = String.valueOf(userId);
//
//        stringRedisTemplate.opsForSet().add(memberKey(key), userValue);
//        stringRedisTemplate.opsForHash().put(
//                joinedAtKey(key),
//                userValue,
//                String.valueOf(System.currentTimeMillis())
//        );
//    }
//
//    public void markProcessing(String queueKey, Long userId) {
//
//        if (userId == null) return;
//
//        stringRedisTemplate.opsForSet()
//                .add(processingKey(queueKey), String.valueOf(userId));
//    }
//
//    public void removeProcessing(String queueKey, Long userId) {
//
//        if (userId == null) return;
//
//        stringRedisTemplate.opsForSet()
//                .remove(processingKey(queueKey), String.valueOf(userId));
//    }
//
//    public void removeUserTracking(String queueKey, Long userId) {
//
//        if (userId == null) return;
//
//        String userValue = String.valueOf(userId);
//
//        stringRedisTemplate.opsForSet().remove(memberKey(queueKey), userValue);
//        stringRedisTemplate.opsForHash().delete(joinedAtKey(queueKey), userValue);
//        stringRedisTemplate.opsForSet().remove(processingKey(queueKey), userValue);
//        stringRedisTemplate.delete(userLockKey(userId));
//    }
//
//    public boolean tryLockQueue(String queueKey) {
//
//        Boolean locked =
//                stringRedisTemplate.opsForValue()
//                        .setIfAbsent(
//                                queueLockKey(queueKey),
//                                "LOCKED",
//                                Duration.ofSeconds(30)
//                        );
//
//        return Boolean.TRUE.equals(locked);
//    }
//
//    public void unlockQueue(String queueKey) {
//        stringRedisTemplate.delete(queueLockKey(queueKey));
//    }
//
//    public void removeActiveQueue(String key) {
//        stringRedisTemplate.opsForSet().remove(ACTIVE_QUEUE_KEY, key);
//    }
//
//    public QueueMeta parseQueueKey(String key) {
//
//        String[] parts = key.split(":");
//
//        if (parts.length != 3) {
//            throw new BusinessException("Invalid queue key: " + key);
//        }
//
//        return new QueueMeta(parts[1], parts[2]);
//    }
//
//    public void cleanupQueue(String queueKey) {
//
//        Set<String> members =
//                stringRedisTemplate.opsForSet().members(memberKey(queueKey));
//
//        if (members == null || members.isEmpty()) {
//            removeActiveQueue(queueKey);
//            return;
//        }
//
//        long staleBefore =
//                System.currentTimeMillis() - USER_STALE_MINUTES * 60 * 1000;
//
//        for (String userIdValue : members) {
//
//            Object joinedAtValue =
//                    stringRedisTemplate.opsForHash()
//                            .get(joinedAtKey(queueKey), userIdValue);
//
//            if (joinedAtValue == null) {
//                removeUserTracking(queueKey, Long.valueOf(userIdValue));
//                continue;
//            }
//
//            long joinedAt = Long.parseLong(joinedAtValue.toString());
//
//            if (joinedAt < staleBefore) {
//                Long userId = Long.valueOf(userIdValue);
//
//                redisTemplate.opsForList().remove(queueKey, 0, userId);
//                removeUserTracking(queueKey, userId);
//            }
//        }
//
//        Long size = redisTemplate.opsForList().size(queueKey);
//
//        if (size == null || size == 0) {
//            removeActiveQueue(queueKey);
//        }
//    }
//}



















//Oldest v.1

//@Service
//@RequiredArgsConstructor
//@Slf4j
//public class MatchQueueService {
//
//    private static final String ACTIVE_QUEUE_KEY = "queue:active";
//    private static final long USER_STALE_MINUTES = 5;
//
//    private final RedisTemplate<String, Long> redisTemplate;
//    private final StringRedisTemplate stringRedisTemplate;
//
//    private String queueKey(String topic, String level) {
//        return "queue:" + normalize(topic) + ":" + normalize(level);
//    }
//
//    private String normalize(String value) {
//        return value.trim().toUpperCase().replace(" ", "_");
//    }
//
//    private String memberKey(String queueKey) {
//        return queueKey + ":members";
//    }
//
//    private String joinedAtKey(String queueKey) {
//        return queueKey + ":joined-at";
//    }
//
//    public void addUserToQueue(String topic, String level, Long userId) {
//
//        String key = queueKey(topic, level);
//        String userValue = String.valueOf(userId);
//
//        if (Boolean.TRUE.equals(stringRedisTemplate.opsForSet().isMember(memberKey(key), userValue))) {
//            log.info("User {} already exists in queue {}", userId, key);
//            return;
//        }
//        redisTemplate.opsForList().remove(key, 0, userId);
//
//        redisTemplate.opsForList().rightPush(key, userId);
//        stringRedisTemplate.opsForSet().add(memberKey(key), userValue);
//        stringRedisTemplate.opsForHash().put(joinedAtKey(key), userValue, String.valueOf(System.currentTimeMillis()));
//        stringRedisTemplate.opsForSet().add(ACTIVE_QUEUE_KEY, key);
//    }
//
//    public Set<String> getActiveQueues() {
//        return stringRedisTemplate.opsForSet().members(ACTIVE_QUEUE_KEY);
//    }
//
//    public Long getQueueSizeByKey(String key) {
//        syncQueueTracking(key);
//        Long queueSize = redisTemplate.opsForList().size(key);
//
//        Long memberSize = stringRedisTemplate.opsForSet().size(memberKey(key));
//
//        Long joinedAtSize = stringRedisTemplate.opsForHash().size(joinedAtKey(key));
//
//        Long activeQueueSize = stringRedisTemplate.opsForSet().size(ACTIVE_QUEUE_KEY);
//
//        log.info(
//                "queue={} | listSize={} | memberSize={} | joinedAtSize={} | activeQueueSize={}",
//                key,
//                queueSize,
//                memberSize,
//                joinedAtSize,
//                activeQueueSize
//        );
//        return redisTemplate.opsForList().size(key);
//    }
//
//    public void syncQueueTracking(String key) {
//
//        List<Long> queueUsers = redisTemplate.opsForList().range(key, 0, -1);
//        Set<String> memberUsers = stringRedisTemplate.opsForSet().members(memberKey(key));
//
//        if (queueUsers == null) queueUsers = List.of();
//
//        Set<String> actualUsers = queueUsers.stream().map(user -> String.valueOf(user)).collect(Collectors.toSet());
//
//        if (memberUsers != null) {
//            for (String member : memberUsers) {
//                if (!actualUsers.contains(member)) {
//                    log.info("Making queue sync");
//                    stringRedisTemplate.opsForSet().remove(memberKey(key), member);
//                    stringRedisTemplate.opsForHash().delete(joinedAtKey(key), member);
//                }
//            }
//        }
//
//        if (actualUsers.isEmpty()) stringRedisTemplate.opsForSet().remove(ACTIVE_QUEUE_KEY, key);
//    }
//
//    public Long popUserByKey(String key) {
//        return redisTemplate.opsForList().leftPop(key);
//    }
//
//    public void removeUserTracking(String key, Long userId) {
//
//        if (userId == null) return;
//
//        String userValue = String.valueOf(userId);
//
//        stringRedisTemplate.opsForSet().remove(memberKey(key), userValue);
//        stringRedisTemplate.opsForHash().delete(joinedAtKey(key), userValue);
//    }
//
//    public void addUserBackToFront(String key, Long userId) {
//
//        if (userId == null) return;
//
//        String userValue = String.valueOf(userId);
//
//        redisTemplate.opsForList().leftPush(key, userId);
//        stringRedisTemplate.opsForSet().add(memberKey(key), userValue);
//        stringRedisTemplate.opsForHash().put(joinedAtKey(key), userValue, String.valueOf(System.currentTimeMillis()));
//    }
//
//    public void removeActiveQueue(String key) {
//        stringRedisTemplate.opsForSet().remove(ACTIVE_QUEUE_KEY, key);
//    }
//
//    public QueueMeta parseQueueKey(String key) {
//
//        String[] parts = key.split(":");
//
//        if (parts.length != 3) {
//            throw new BusinessException("Invalid queue key: " + key);
//        }
//
//        return new QueueMeta(parts[1], parts[2]);
//    }
//
//    public boolean tryLockQueue(String queueKey) {
//
//        Boolean locked = stringRedisTemplate
//                .opsForValue()
//                .setIfAbsent("lock:" + queueKey, "LOCKED", Duration.ofSeconds(20));
//
//        return Boolean.TRUE.equals(locked);
//    }
//
//    public void unlockQueue(String queueKey) {
//        stringRedisTemplate.delete("lock:" + queueKey);
//    }
//
//    public boolean isUserStale(String queueKey, Long userId) {
//
//        Object value = stringRedisTemplate
//                .opsForHash()
//                .get(joinedAtKey(queueKey), String.valueOf(userId));
//
//        if (value == null) return false;
//
//        long joinedAt = Long.parseLong(value.toString());
//        long staleAfter = System.currentTimeMillis() - USER_STALE_MINUTES * 6 * 1000;
//
//        return joinedAt < staleAfter;
//    }
//}
