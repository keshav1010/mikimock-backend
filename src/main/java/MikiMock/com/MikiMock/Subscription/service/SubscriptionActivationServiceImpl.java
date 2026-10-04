package MikiMock.com.MikiMock.Subscription.service;

import MikiMock.com.MikiMock.Common.Exception.BusinessException;
import MikiMock.com.MikiMock.Notification.dto.NotificationEvent;
import MikiMock.com.MikiMock.Notification.producer.NotificationProducer;
import MikiMock.com.MikiMock.Payment.entity.Payment;
import MikiMock.com.MikiMock.Payment.entity.PaymentStatus;
import MikiMock.com.MikiMock.Subscription.entity.UserSubscription;
import MikiMock.com.MikiMock.Subscription.entity.UserSubscriptionStatus;
import MikiMock.com.MikiMock.Subscription.repository.UserSubscriptionRepository;
import MikiMock.com.MikiMock.User.entity.SubscriptionType;
import MikiMock.com.MikiMock.User.entity.User;
import MikiMock.com.MikiMock.User.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SubscriptionActivationServiceImpl implements SubscriptionActivationService {

    private final UserSubscriptionRepository subscriptionRepository;
    private final UserRepository userRepository;
    private final NotificationProducer notificationProducer;

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public UserSubscription activateSubscription(Payment payment) {

        if (payment.getStatus() != PaymentStatus.SUCCESS) {
            throw new BusinessException(
                    "Cannot activate subscription for unsuccessful payment"
            );
        }

        User user = userRepository
                .findByIdForUpdate(payment.getUser().getId())
                .orElseThrow(
                        () -> new BusinessException("User not found")
                );

        // The payment is locked by the calling persistence service.
        // This check protects against duplicate activation.
        var existing = subscriptionRepository
                .findByPaymentId(payment.getId());

        if (existing.isPresent()) {
            log.info(
                    "Subscription already activated | paymentId={}",
                    payment.getId()
            );

            return existing.get();
        }

        LocalDateTime now = LocalDateTime.now();

        // Expire outdated ACTIVE records before calculating renewal.
        List<UserSubscription> subscriptions =
                subscriptionRepository.findLifecycleSubscriptions(
                        user.getId(),
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
            }
        }

        subscriptionRepository.saveAll(subscriptions);

        // Preserve remaining Premium time and already queued renewals.
        LocalDateTime latestExpiry =
                subscriptionRepository.findLatestExpiry(
                        user.getId(),
                        List.of(
                                UserSubscriptionStatus.ACTIVE,
                                UserSubscriptionStatus.SCHEDULED
                        ),
                        now
                ).orElse(now);

        LocalDateTime startsAt =
                latestExpiry.isAfter(now) ? latestExpiry : now;

        LocalDateTime expiresAt =
                SubscriptionDuration.calculateExpiry(
                        payment.getSubscriptionPlan().getPlanCode(),
                        startsAt
                );

        UserSubscriptionStatus status =
                startsAt.isAfter(now)
                        ? UserSubscriptionStatus.SCHEDULED
                        : UserSubscriptionStatus.ACTIVE;

        UserSubscription subscription =
                UserSubscription.builder()
                        .user(user)
                        .subscriptionPlan(payment.getSubscriptionPlan())
                        .payment(payment)
                        .status(status)
                        .startsAt(startsAt)
                        .expiresAt(expiresAt)
                        .build();

        subscriptionRepository.saveAndFlush(subscription);

        user.setSubscriptionType(SubscriptionType.PREMIUM);
        userRepository.save(user);

        log.info(
                "Subscription activated | userId={} | paymentId={} | status={} | startsAt={} | expiresAt={}",
                user.getId(),
                payment.getId(),
                status,
                startsAt,
                expiresAt
        );

        NotificationEvent event = NotificationEvent.builder()

                .toEmail(user.getEmail())

                .subject("Your MikiMock Premium Subscription Is Active")

                .body(
                        """
                        Hi,
        
                        Thank you for upgrading to MikiMock Premium!
        
                        Your payment has been successfully processed, and your Premium subscription is now active.
        
                        You can now access all Premium features available with your subscription plan.
        
                        We hope MikiMock helps you practice effectively, improve your interview skills, and prepare with confidence.
        
                        Thank you for choosing MikiMock.
        
                        Best regards,
                        Team MikiMock
        
                        Note: If you notice any mismatch in your subscription details or experience any payment-related issue, please contact us through the Contact Us page.
                        """
                )

                .build();

        notificationProducer.publish(event);

        return subscription;
    }
}