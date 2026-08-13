package MikiMock.com.MikiMock.Notification.cache;


import MikiMock.com.MikiMock.Common.constants.RedisKeys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationIdempotencyServiceImp implements NotificationIdempotencyService{


    private static final Duration TTL = Duration.ofDays(15);
    private final RedisTemplate<String , String> redisTemplate;


    @Override
    public boolean isProcessed(String eventId) {
        String key = RedisKeys.NOTIFICATION_PROCESSED + eventId;

        Boolean exists = redisTemplate.hasKey(key);

        return Boolean.TRUE.equals(exists);
    }

    @Override
    public void markProcessed(String eventId) {
        String key = RedisKeys.NOTIFICATION_PROCESSED + eventId;
        redisTemplate.opsForValue().set(
                key,
                "PROCESSED",
                TTL
        );

        log.info("Notification marked processed | eventId = {}",eventId);
    }
}
