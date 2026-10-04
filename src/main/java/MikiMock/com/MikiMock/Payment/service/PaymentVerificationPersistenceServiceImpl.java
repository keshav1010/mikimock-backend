package MikiMock.com.MikiMock.Payment.service;

import MikiMock.com.MikiMock.Common.Exception.BusinessException;
import MikiMock.com.MikiMock.Payment.entity.Payment;
import MikiMock.com.MikiMock.Payment.entity.PaymentProviderType;
import MikiMock.com.MikiMock.Payment.entity.PaymentStatus;
import MikiMock.com.MikiMock.Payment.repository.PaymentRepository;
import MikiMock.com.MikiMock.Subscription.service.SubscriptionActivationService;
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
public class PaymentVerificationPersistenceServiceImpl implements PaymentVerificationPersistenceService {

    private final PaymentRepository paymentRepository;
    private final SubscriptionActivationService subscriptionActivationService;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public void markPaymentSuccessful(
            Payment payment,
            String providerPaymentId,
            String signature
    ) {

        /*
         * Protect against duplicate execution.
         */
        if (
                payment.getStatus()
                        == PaymentStatus.SUCCESS
        ) {

            log.info(
                    "Payment already marked successful | paymentId={}",
                    payment.getId()
            );

            return;
        }

        payment.setProviderPaymentId(
                providerPaymentId
        );

        payment.setProviderSignature(
                signature
        );

        payment.setStatus(
                PaymentStatus.SUCCESS
        );

        paymentRepository.save(payment);
        subscriptionActivationService.activateSubscription(payment);

    }

    @Override
    @Transactional
    public void markPaymentFailed(
            Payment payment
    ) {

        /*
         * Never downgrade a successful payment.
         */
        if (
                payment.getStatus()
                        == PaymentStatus.SUCCESS
        ) {

            return;
        }

        payment.setStatus(
                PaymentStatus.FAILED
        );

        paymentRepository.save(payment);

        log.info(
                "Payment marked failed | paymentId={}",
                payment.getId()
        );
    }

    @Transactional
    @Override
    public void markWebhookPaymentSuccessful(
            PaymentProviderType provider,
            String providerOrderId,
            String providerPaymentId
    ) {

        Payment payment = paymentRepository
                .findByProviderOrderIdForUpdate(
                        providerOrderId
                )
                .orElseThrow(
                        () -> new BusinessException(
                                "Payment not found"
                        )
                );

        /*
         * Security check.
         */
        if (payment.getProvider() != provider) {

            log.error(
                    "Webhook payment provider mismatch | paymentId={} | expected={} | received={}",
                    payment.getId(),
                    payment.getProvider(),
                    provider
            );

            throw new BusinessException(
                    "Payment provider mismatch"
            );
        }

        /*
         * Idempotency.
         */
        if (payment.getStatus() == PaymentStatus.SUCCESS) {

            log.info(
                    "Webhook payment already successful | paymentId={}",
                    payment.getId()
            );

            return;
        }

        /*
         * Store provider payment ID.
         */
        if (providerPaymentId != null) {

            payment.setProviderPaymentId(providerPaymentId);
        }

        /*
         * Mark successful.
         */
        payment.setStatus(PaymentStatus.SUCCESS);
        paymentRepository.save(payment);
        /*
         * Activate subscription.
         */
        subscriptionActivationService.activateSubscription(payment);

        log.info(
                "Webhook payment marked successful | paymentId={} | providerPaymentId={}",
                payment.getId(),
                payment.getProviderPaymentId()
        );
    }


    @Transactional
    @Override
    public void markWebhookPaymentFailed(
            PaymentProviderType provider,
            String providerOrderId,
            String providerPaymentId
    ) {

        Payment payment = paymentRepository
                .findByProviderOrderIdForUpdate(
                        providerOrderId
                )
                .orElseThrow(
                        () -> new BusinessException(
                                "Payment not found"
                        )
                );

        /*
         * Security check.
         */
        if (payment.getProvider() != provider) {

            log.error(
                    "Webhook payment provider mismatch | paymentId={} | expected={} | received={}",
                    payment.getId(),
                    payment.getProvider(),
                    provider
            );

            throw new BusinessException(
                    "Payment provider mismatch"
            );
        }

        /*
         * Never downgrade a successful payment.
         */
        if (payment.getStatus() == PaymentStatus.SUCCESS) {

            log.warn(
                    "Ignoring failed webhook for successful payment | paymentId={}",
                    payment.getId()
            );

            return;
        }

        /*
         * Store provider payment ID.
         */
        if (providerPaymentId != null) {

            payment.setProviderPaymentId(
                    providerPaymentId
            );
        }

        payment.setStatus(
                PaymentStatus.FAILED
        );

        paymentRepository.save(payment);

        log.info(
                "Webhook payment marked failed | paymentId={} | providerPaymentId={}",
                payment.getId(),
                payment.getProviderPaymentId()
        );
    }

    @Transactional
    @Override
    public void reconcilePaymentSuccess(
            PaymentProviderType provider,
            String providerOrderId,
            String providerPaymentId
    ) {

        Payment payment =
                paymentRepository
                        .findByProviderOrderIdForUpdate(
                                providerOrderId
                        )
                        .orElseThrow(
                                () -> new BusinessException(
                                        "Payment not found"
                                )
                        );


        if (payment.getProvider() != provider) {

            throw new BusinessException(
                    "Payment provider mismatch"
            );
        }


        /*
         * Another flow may have already
         * successfully processed the payment.
         */
        if (payment.getStatus() == PaymentStatus.SUCCESS) {

            log.info(
                    "Reconciliation ignored | payment already successful | paymentId={}",
                    payment.getId()
            );

            return;
        }


        if (providerPaymentId != null) {

            payment.setProviderPaymentId(
                    providerPaymentId
            );
        }


        payment.setStatus(
                PaymentStatus.SUCCESS
        );

        paymentRepository.save(payment);
        subscriptionActivationService.activateSubscription(payment);

        log.info(
                "Payment reconciled successfully | paymentId={} | providerOrderId={}",
                payment.getId(),
                providerOrderId
        );
    }

    @Transactional
    @Override
    public void reconcilePaymentFailure(
            PaymentProviderType provider,
            String providerOrderId,
            String providerPaymentId
    ) {

        Payment payment =
                paymentRepository
                        .findByProviderOrderIdForUpdate(
                                providerOrderId
                        )
                        .orElseThrow(
                                () -> new BusinessException(
                                        "Payment not found"
                                )
                        );


        if (payment.getProvider() != provider) {

            throw new BusinessException(
                    "Payment provider mismatch"
            );
        }


        /*
         * Never downgrade a successful payment.
         */
        if (payment.getStatus() == PaymentStatus.SUCCESS) {

            log.warn(
                    "Ignoring reconciliation failure | payment already successful | paymentId={}",
                    payment.getId()
            );

            return;
        }


        if (providerPaymentId != null) {

            payment.setProviderPaymentId(
                    providerPaymentId
            );
        }


        payment.setStatus(
                PaymentStatus.FAILED
        );

        paymentRepository.save(payment);


        log.info(
                "Payment reconciled as failed | paymentId={} | providerOrderId={}",
                payment.getId(),
                providerOrderId
        );
    }
}