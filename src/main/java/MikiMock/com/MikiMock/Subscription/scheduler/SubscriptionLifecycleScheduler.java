package MikiMock.com.MikiMock.Subscription.scheduler;

import MikiMock.com.MikiMock.Subscription.entity.UserSubscriptionStatus;
import MikiMock.com.MikiMock.Subscription.repository.UserSubscriptionRepository;
import MikiMock.com.MikiMock.Subscription.service.SubscriptionLifecycleService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class SubscriptionLifecycleScheduler {

    private static final int BATCH_SIZE = 100;

    private final UserSubscriptionRepository subscriptionRepository;
    private final SubscriptionLifecycleService lifecycleService;

    @Scheduled(fixedDelay = 60_000)
    public void processSubscriptions() {

        LocalDateTime now = LocalDateTime.now();

        List<Long> userIds =
                subscriptionRepository.findUsersNeedingLifecycleUpdate(
                        UserSubscriptionStatus.ACTIVE,
                        UserSubscriptionStatus.SCHEDULED,
                        now,
                        PageRequest.of(0, BATCH_SIZE)
                );

        for (Long userId : userIds) {

            try {
                lifecycleService.processUserSubscriptions(userId);

            } catch (Exception exception) {

                log.error(
                        "Subscription lifecycle processing failed | userId={}",
                        userId,
                        exception
                );
            }
        }

        log.info(
                "Subscription lifecycle batch completed | processed={}",
                userIds.size()
        );
    }
}