package MikiMock.com.MikiMock.Analytics.cache;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RedisAnalyticsServiceImpl implements RedisAnalyticsService {

    private final RedisTemplate<String, Long> redisTemplate;

    @Override
    public Long getCounter(String key) {

        Object value = redisTemplate.opsForValue().get(key);

        if (value == null) {

            return null;
        }

        return Long.parseLong(value.toString());
    }

    @Override
    public void setCounter(String key,Long value) {

        redisTemplate.opsForValue().set(key, value);

    }

    @Override
    public void increment(String key) {

        redisTemplate
                .opsForValue()
                .increment(key);

    }

    @Override
    public void decrement(String key) {

        redisTemplate
                .opsForValue()
                .decrement(key);

    }

}
