package MikiMock.com.MikiMock.Analytics.service;

import MikiMock.com.MikiMock.Analytics.cache.RedisAnalyticsService;
import MikiMock.com.MikiMock.Analytics.dto.DashboardResponse;
import MikiMock.com.MikiMock.Analytics.dto.OverviewDto;
import MikiMock.com.MikiMock.Analytics.dto.SystemHealthDto;
import MikiMock.com.MikiMock.Analytics.dto.TodayDto;
import MikiMock.com.MikiMock.Common.constants.RedisKeys;
import MikiMock.com.MikiMock.Interview.Repository.InterviewRepository;
import MikiMock.com.MikiMock.Room.repository.RoomRepository;
import MikiMock.com.MikiMock.User.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnalyticsServiceImpl implements AnalyticsService {

    private final UserRepository userRepository;

    private final InterviewRepository interviewRepository;

    private final RoomRepository roomRepository;

    private final RedisAnalyticsService redisAnalyticsService;

    private final HealthCheckService healthCheckService;

    private final Executor analyticsExecutor;

    @Override
    public DashboardResponse getDashboard() {

        CompletableFuture<OverviewDto> overviewFuture =
                CompletableFuture.supplyAsync(
                        this::getOverview,
                        analyticsExecutor
                );

        CompletableFuture<TodayDto> todayFuture =
                CompletableFuture.supplyAsync(
                        this::getToday,
                        analyticsExecutor
                );

        CompletableFuture<SystemHealthDto> systemFuture =
                CompletableFuture.supplyAsync(
                        this::getSystemHealth,
                        analyticsExecutor
                );

        CompletableFuture.allOf(
                overviewFuture,
                todayFuture,
                systemFuture
        ).join();

        return DashboardResponse.builder()
                .overview(overviewFuture.join())
                .today(todayFuture.join())
                .system(systemFuture.join())
                .build();
    }

    private OverviewDto getOverview() {

        return OverviewDto.builder()

                .totalUsers(
                        redisAnalyticsService.getCounter(
                                RedisKeys.TOTAL_USERS
                        )
                )

                .premiumUsers(
                        redisAnalyticsService.getCounter(
                                RedisKeys.PREMIUM_USERS
                        )
                )

                .freeUsers(
                        redisAnalyticsService.getCounter(
                                RedisKeys.FREE_USERS
                        )
                )

                .totalInterviews(
                        redisAnalyticsService.getCounter(
                                RedisKeys.TOTAL_INTERVIEWS
                        )
                )

                .activeInterviews(
                        redisAnalyticsService.getCounter(
                                RedisKeys.ACTIVE_INTERVIEWS
                        )
                )

                .waitingUsers(
                        redisAnalyticsService.getCounter(
                                RedisKeys.WAITING_USERS
                        )
                )

                .build();
    }

    private TodayDto getToday() {

        LocalDate today = LocalDate.now();

        LocalDateTime start = today.atStartOfDay();

        LocalDateTime end = start.plusDays(1);

        CompletableFuture<Long> usersFuture =
                CompletableFuture.supplyAsync(
                        () -> userRepository.getRegisteredToday(start, end),
                        analyticsExecutor
                );

        CompletableFuture<Long> interviewFuture =
                CompletableFuture.supplyAsync(
                        interviewRepository::getCompletedInterviewToday,
                        analyticsExecutor
                );

        CompletableFuture<Long> roomFuture =
                CompletableFuture.supplyAsync(
                        () -> roomRepository.roomsCreatedToday(start,end),
                        analyticsExecutor
                );

        CompletableFuture<Long> noShowFuture =
                CompletableFuture.supplyAsync(
                        interviewRepository::noShowToday,
                        analyticsExecutor
                );

        CompletableFuture.allOf(
                usersFuture,
                interviewFuture,
                roomFuture,
                noShowFuture
        ).join();

        return TodayDto.builder()

                .usersRegisteredToday(
                        usersFuture.join()
                )

                .interviewsCompletedToday(
                        interviewFuture.join()
                )

                .roomsCreatedToday(
                        roomFuture.join()
                )

                .noShowToday(
                        noShowFuture.join()
                )

                .build();
    }

    private SystemHealthDto getSystemHealth() {

        CompletableFuture<Boolean> database =
                CompletableFuture.supplyAsync(
                        healthCheckService::isDatabaseUp,
                        analyticsExecutor
                );

        CompletableFuture<Boolean> redis =
                CompletableFuture.supplyAsync(
                        healthCheckService::isRedisUp,
                        analyticsExecutor
                );

        CompletableFuture<Boolean> kafka =
                CompletableFuture.supplyAsync(
                        healthCheckService::isKafkaUp,
                        analyticsExecutor
                );

        CompletableFuture<Boolean> mail =
                CompletableFuture.supplyAsync(
                        healthCheckService::isMailUp,
                        analyticsExecutor
                );

        CompletableFuture<Boolean> application =
                CompletableFuture.supplyAsync(
                        healthCheckService::isApplicationUp,
                        analyticsExecutor
                );

        CompletableFuture.allOf(
                database,
                redis,
                kafka,
                mail,
                application
        ).join();

        return SystemHealthDto.builder()

                .database(
                        database.join()
                )

                .redis(
                        redis.join()
                )

                .kafka(
                        kafka.join()
                )

                .mail(
                        mail.join()
                )

                .application(
                        application.join()
                )

                .build();
    }
}