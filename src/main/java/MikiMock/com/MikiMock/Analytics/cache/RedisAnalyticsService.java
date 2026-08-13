package MikiMock.com.MikiMock.Analytics.cache;


public interface RedisAnalyticsService {

    Long getCounter(String key);

    void setCounter(String key, Long value);

    void increment(String key);

    void decrement(String key);

}
