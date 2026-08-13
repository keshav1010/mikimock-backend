package MikiMock.com.MikiMock.Analytics.initializer;

import MikiMock.com.MikiMock.Analytics.cache.RedisAnalyticsService;
import MikiMock.com.MikiMock.Common.constants.RedisKeys;
import MikiMock.com.MikiMock.Interview.Repository.InterviewRepository;
import MikiMock.com.MikiMock.User.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AnalyticsCacheInitializer {

    private final UserRepository userRepository;

    private final InterviewRepository interviewRepository;

    private final RedisAnalyticsService redisAnalyticsService;

    @EventListener(ApplicationReadyEvent.class)
    public void initializeAnalyticsCache() {

        redisAnalyticsService.setCounter(
                RedisKeys.TOTAL_USERS,
                userRepository.count());

        redisAnalyticsService.setCounter(
                RedisKeys.PREMIUM_USERS,
                userRepository.countPremiumUsers());

        redisAnalyticsService.setCounter(
                RedisKeys.FREE_USERS,
                userRepository.countFreeUsers());

        redisAnalyticsService.setCounter(
                RedisKeys.TOTAL_INTERVIEWS,
                interviewRepository.count());

        redisAnalyticsService.setCounter(
                RedisKeys.ACTIVE_INTERVIEWS,
                interviewRepository.countRunning());

    }

}