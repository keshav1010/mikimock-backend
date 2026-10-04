package MikiMock.com.MikiMock.Subscription.service;

import MikiMock.com.MikiMock.Subscription.entity.UserSubscription;
import MikiMock.com.MikiMock.Subscription.entity.UserSubscriptionStatus;
import MikiMock.com.MikiMock.Subscription.repository.UserSubscriptionRepository;
import MikiMock.com.MikiMock.User.entity.SubscriptionType;
import MikiMock.com.MikiMock.User.entity.User;
import MikiMock.com.MikiMock.User.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SubscriptionLifecycleServiceImpl
        implements SubscriptionLifecycleService {

    private final UserSubscriptionRepository subscriptionRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public void processUserSubscriptions(Long userId) {

        // Same user lock used during payment activation.
        User user = userRepository
                .findByIdForUpdate(userId)
                .orElseThrow();

        LocalDateTime now = LocalDateTime.now();

        List<UserSubscription> subscriptions =
                subscriptionRepository.findLifecycleSubscriptions(
                        userId,
                        List.of(
                                UserSubscriptionStatus.ACTIVE,
                                UserSubscriptionStatus.SCHEDULED
                        )
                );

        for (UserSubscription subscription : subscriptions) {

            if (!subscription.getExpiresAt().isAfter(now)) {

                subscription.setStatus(
                        UserSubscriptionStatus.EXPIRED
                );

            } else if (
                    subscription.getStatus()
                            == UserSubscriptionStatus.SCHEDULED
                            && !subscription.getStartsAt().isAfter(now)
            ) {

                subscription.setStatus(
                        UserSubscriptionStatus.ACTIVE
                );
            }
        }

        subscriptionRepository.saveAll(subscriptions);

        boolean hasPremium =
                subscriptionRepository.hasValidSubscription(
                        userId,
                        UserSubscriptionStatus.ACTIVE,
                        now
                );

        SubscriptionType newType =
                hasPremium
                        ? SubscriptionType.PREMIUM
                        : SubscriptionType.FREE;

        if (user.getSubscriptionType() != newType) {

            user.setSubscriptionType(newType);
            userRepository.save(user);

            log.info(
                    "Subscription access changed | userId={} | subscriptionType={}",
                    userId,
                    newType
            );
        }
    }
}