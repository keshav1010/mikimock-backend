package MikiMock.com.MikiMock.Payment.service;

import MikiMock.com.MikiMock.Common.Exception.BusinessException;

import MikiMock.com.MikiMock.Payment.dto.ProviderWebhookEvent;

import MikiMock.com.MikiMock.Payment.entity.Payment;
import MikiMock.com.MikiMock.Payment.entity.PaymentProviderType;
import MikiMock.com.MikiMock.Payment.entity.PaymentStatus;

import MikiMock.com.MikiMock.Payment.factory.PaymentProviderFactory;
import MikiMock.com.MikiMock.Payment.gateway.PaymentGateway;

import MikiMock.com.MikiMock.Payment.repository.PaymentRepository;

import MikiMock.com.MikiMock.User.entity.SubscriptionType;
import MikiMock.com.MikiMock.User.entity.User;

import MikiMock.com.MikiMock.User.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentWebhookServiceImpl implements PaymentWebhookService {

    private final PaymentProviderFactory paymentProviderFactory;

    private final PaymentRepository paymentRepository;

    private final UserRepository userRepository;

    private final PaymentVerificationPersistenceService paymentVerificationPersistenceService;


    @Override
    @Transactional
    public void processWebhook(PaymentProviderType provider, String signature, String rawPayload) {

        /*
         * 1. Get correct provider.
         *
         * RAZORPAY -> RazorpayPaymentGateway
         * PAYPAL   -> PayPalPaymentGateway later
         */
        PaymentGateway paymentGateway = paymentProviderFactory.getProvider(provider);

        /*
         * 2. Verify signature BEFORE reading/trusting
         * the webhook payload.
         */
        boolean validSignature = paymentGateway.verifyWebhookSignature(rawPayload, signature);

        if (!validSignature) {
            log.warn("Invalid webhook signature | provider={}", provider);
            throw new BusinessException("Invalid webhook signature");
        }


        /*
         * 3. Parse provider-specific webhook.
         */
        ProviderWebhookEvent webhookEvent = paymentGateway.parseWebhookEvent(rawPayload);


        log.info("Payment webhook received | provider={} | eventType={} | providerOrderId={} | providerPaymentId={}",
                provider, webhookEvent.getEventType(), webhookEvent.getProviderOrderId(), webhookEvent.getProviderPaymentId()
        );


        /*
         * 4. Security validation.
         *
         * Factory provider and parsed provider must match.
         */
        if (webhookEvent.getProvider() != provider) {

            log.error("Webhook provider mismatch | requestProvider={} | payloadProvider={}",
                    provider, webhookEvent.getProvider()
            );

            throw new BusinessException("Webhook provider mismatch");
        }



        /*
         * 7. Process supported events.
         */
        switch (webhookEvent.getEventType()) {

            case "payment.captured" -> paymentVerificationPersistenceService
                    .markWebhookPaymentSuccessful(
                            provider,
                            webhookEvent.getProviderOrderId(),
                            webhookEvent.getProviderPaymentId()
                    );
            case "payment.failed" -> paymentVerificationPersistenceService
                    .markWebhookPaymentFailed(
                            provider,
                            webhookEvent.getProviderOrderId(),
                            webhookEvent.getProviderPaymentId()
                    );


            default -> log.info("Ignoring unsupported webhook event | provider={} | eventType={} | providerOrderId={}",
                    provider, webhookEvent.getEventType(), webhookEvent.getProviderOrderId()
            );
        }
    }


}