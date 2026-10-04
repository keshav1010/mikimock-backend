package MikiMock.com.MikiMock.Subscription.service;

import MikiMock.com.MikiMock.Payment.entity.Payment;
import MikiMock.com.MikiMock.Subscription.entity.UserSubscription;

public interface SubscriptionActivationService {

    UserSubscription activateSubscription(Payment payment);
}