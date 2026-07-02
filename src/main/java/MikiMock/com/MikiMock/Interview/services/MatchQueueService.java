package MikiMock.com.MikiMock.Interview.services;


import MikiMock.com.MikiMock.Common.Exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;



@Service
@RequiredArgsConstructor
@Slf4j
public class MatchQueueService {

    private static final String ACTIVE_QUEUE_KEY = "queue:active";
    private static final long USER_STALE_MINUTES = 5;

    private final RedisTemplate<String, Long> redisTemplate;
    private final StringRedisTemplate stringRedisTemplate;

    private String queueKey(String topic, String level) {
        return "queue:" + normalize(topic) + ":" + normalize(level);
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

    private String queueLockKey(String queueKey) {
        return "lock:" + queueKey;
    }

    private String normalize(String value) {
        return value.trim().toUpperCase().replace(" ", "_");
    }

    public void addUserToQueue(String topic, String level, Long userId) {

        String key = queueKey(topic, level);

        Boolean locked =
                stringRedisTemplate.opsForValue()
                        .setIfAbsent(
                                userLockKey(userId),
                                key,
                                Duration.ofMinutes(USER_STALE_MINUTES)
                        );

        if (!Boolean.TRUE.equals(locked)) {
            log.info("User {} already exists in matchmaking", userId);
            return;
        }

        redisTemplate.opsForList().remove(key, 0, userId);
        redisTemplate.opsForList().rightPush(key, userId);

        String userValue = String.valueOf(userId);

        stringRedisTemplate.opsForSet().add(memberKey(key), userValue);
        stringRedisTemplate.opsForHash().put(
                joinedAtKey(key),
                userValue,
                String.valueOf(System.currentTimeMillis())
        );

        stringRedisTemplate.opsForSet().add(ACTIVE_QUEUE_KEY, key);
    }

    public Set<String> getActiveQueues() {
        return stringRedisTemplate.opsForSet().members(ACTIVE_QUEUE_KEY);
    }

    public Long getQueueSizeByKey(String key) {

        Long listSize = redisTemplate.opsForList().size(key);
        Long memberSize = stringRedisTemplate.opsForSet().size(memberKey(key));
        Long processingSize = stringRedisTemplate.opsForSet().size(processingKey(key));

        log.info(
                "queue={} | listSize={} | memberSize={} | processingSize={}",
                key,
                listSize,
                memberSize,
                processingSize
        );

        return listSize;
    }

    public Long popUserByKey(String key) {
        return redisTemplate.opsForList().leftPop(key);
    }

    public void addUserBackToFront(String key, Long userId) {

        if (userId == null) return;

        redisTemplate.opsForList().remove(key, 0, userId);
        redisTemplate.opsForList().leftPush(key, userId);

        String userValue = String.valueOf(userId);

        stringRedisTemplate.opsForSet().add(memberKey(key), userValue);
        stringRedisTemplate.opsForHash().put(
                joinedAtKey(key),
                userValue,
                String.valueOf(System.currentTimeMillis())
        );
    }

    public void markProcessing(String queueKey, Long userId) {

        if (userId == null) return;

        stringRedisTemplate.opsForSet()
                .add(processingKey(queueKey), String.valueOf(userId));
    }

    public void removeProcessing(String queueKey, Long userId) {

        if (userId == null) return;

        stringRedisTemplate.opsForSet()
                .remove(processingKey(queueKey), String.valueOf(userId));
    }

    public void removeUserTracking(String queueKey, Long userId) {

        if (userId == null) return;

        String userValue = String.valueOf(userId);

        stringRedisTemplate.opsForSet().remove(memberKey(queueKey), userValue);
        stringRedisTemplate.opsForHash().delete(joinedAtKey(queueKey), userValue);
        stringRedisTemplate.opsForSet().remove(processingKey(queueKey), userValue);
        stringRedisTemplate.delete(userLockKey(userId));
    }

    public boolean tryLockQueue(String queueKey) {

        Boolean locked =
                stringRedisTemplate.opsForValue()
                        .setIfAbsent(
                                queueLockKey(queueKey),
                                "LOCKED",
                                Duration.ofSeconds(30)
                        );

        return Boolean.TRUE.equals(locked);
    }

    public void unlockQueue(String queueKey) {
        stringRedisTemplate.delete(queueLockKey(queueKey));
    }

    public void removeActiveQueue(String key) {
        stringRedisTemplate.opsForSet().remove(ACTIVE_QUEUE_KEY, key);
    }

    public QueueMeta parseQueueKey(String key) {

        String[] parts = key.split(":");

        if (parts.length != 3) {
            throw new BusinessException("Invalid queue key: " + key);
        }

        return new QueueMeta(parts[1], parts[2]);
    }

    public void cleanupQueue(String queueKey) {

        Set<String> members =
                stringRedisTemplate.opsForSet().members(memberKey(queueKey));

        if (members == null || members.isEmpty()) {
            removeActiveQueue(queueKey);
            return;
        }

        long staleBefore =
                System.currentTimeMillis() - USER_STALE_MINUTES * 60 * 1000;

        for (String userIdValue : members) {

            Object joinedAtValue =
                    stringRedisTemplate.opsForHash()
                            .get(joinedAtKey(queueKey), userIdValue);

            if (joinedAtValue == null) {
                removeUserTracking(queueKey, Long.valueOf(userIdValue));
                continue;
            }

            long joinedAt = Long.parseLong(joinedAtValue.toString());

            if (joinedAt < staleBefore) {
                Long userId = Long.valueOf(userIdValue);

                redisTemplate.opsForList().remove(queueKey, 0, userId);
                removeUserTracking(queueKey, userId);
            }
        }

        Long size = redisTemplate.opsForList().size(queueKey);

        if (size == null || size == 0) {
            removeActiveQueue(queueKey);
        }
    }
}












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
