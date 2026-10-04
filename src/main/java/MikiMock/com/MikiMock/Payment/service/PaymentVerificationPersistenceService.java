package MikiMock.com.MikiMock.Payment.service;

import MikiMock.com.MikiMock.Payment.entity.Payment;
import MikiMock.com.MikiMock.Payment.entity.PaymentProviderType;

public interface PaymentVerificationPersistenceService {

    void markPaymentSuccessful(
            Payment payment,
            String providerPaymentId,
            String signature
    );

    void markPaymentFailed(
            Payment payment
    );

    void markWebhookPaymentSuccessful(
            PaymentProviderType provider,
            String providerOrderId,
            String providerPaymentId
    );

    void markWebhookPaymentFailed(
            PaymentProviderType provider,
            String providerOrderId,
            String providerPaymentId
    );

    void reconcilePaymentSuccess(
            PaymentProviderType provider,
            String providerOrderId,
            String providerPaymentId
    );

    void reconcilePaymentFailure(
            PaymentProviderType provider,
            String providerOrderId,
            String providerPaymentId
    );
}