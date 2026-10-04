package MikiMock.com.MikiMock.Subscription.service;

import MikiMock.com.MikiMock.Payment.entity.SubscriptionPlanCode;

import java.time.LocalDateTime;

public final class SubscriptionDuration {

    private SubscriptionDuration() {
    }

    public static LocalDateTime calculateExpiry(
            SubscriptionPlanCode planCode,
            LocalDateTime startsAt
    ) {
        return switch (planCode) {

            case PREMIUM_MONTHLY ->
                    startsAt.plusMonths(1);

            case PREMIUM_QUARTERLY ->
                    startsAt.plusMonths(3);

            case PREMIUM_HALF_YEARLY ->
                    startsAt.plusMonths(6);

            case PREMIUM_YEARLY ->
                    startsAt.plusYears(1);
        };
    }
}