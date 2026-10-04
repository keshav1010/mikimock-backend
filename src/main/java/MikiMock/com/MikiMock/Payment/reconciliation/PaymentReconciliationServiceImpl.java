package MikiMock.com.MikiMock.Payment.reconciliation;

import MikiMock.com.MikiMock.Payment.dto.ProviderPaymentStatusResponse;
import MikiMock.com.MikiMock.Payment.entity.Payment;
import MikiMock.com.MikiMock.Payment.entity.PaymentStatus;

import MikiMock.com.MikiMock.Payment.factory.PaymentProviderFactory;
import MikiMock.com.MikiMock.Payment.gateway.PaymentGateway;

import MikiMock.com.MikiMock.Payment.repository.PaymentRepository;

import MikiMock.com.MikiMock.Payment.service.PaymentVerificationPersistenceService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.PageRequest;


import org.springframework.stereotype.Service;

import org.springframework.data.domain.Pageable;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentReconciliationServiceImpl implements PaymentReconciliationService {

    private static final int BATCH_SIZE = 50;

    private final PaymentRepository paymentRepository;

    private final PaymentProviderFactory paymentProviderFactory;

    private final PaymentVerificationPersistenceService paymentVerificationPersistenceService;


    @Override
    public void reconcilePendingPayments() {

        /*
         * Don't reconcile payments created
         * in the last 5 minutes.
         *
         * They may still be actively
         * completing checkout.
         */
        LocalDateTime before = LocalDateTime.now().minusMinutes(5);


        List<Payment> payments = paymentRepository
                .findPaymentsForReconciliation(
                        PaymentStatus.PENDING,
                        before,
                        PageRequest.of(
                                0,
                                BATCH_SIZE
                        )
                );


        if (payments.isEmpty()) {
            return;
        }


        log.info(
                "Starting payment reconciliation | pendingPayments={}",
                payments.size()
        );


        /*
         * Process independently.
         *
         * One provider failure must not
         * stop reconciliation for others.
         */
        for (Payment payment : payments) {

            try {
                reconcilePayment(payment);

            } catch (Exception e) {

                log.error(
                        "Payment reconciliation failed | paymentId={} | providerOrderId={}",
                        payment.getId(),
                        payment.getProviderOrderId(),
                        e
                );
            }
        }
    }


    private void reconcilePayment(Payment payment) {

        PaymentGateway gateway = paymentProviderFactory.getProvider(payment.getProvider());

        log.info("gateway : {}" , gateway.getProviderType());

        /*
         * External API call.
         *
         * No DB transaction here.
         */
        ProviderPaymentStatusResponse providerResponse = gateway.getPaymentStatus(payment.getProviderOrderId());

        log.info("payment status: {}" , providerResponse.getStatus());


        switch (
                providerResponse.getStatus()
        ) {

            case SUCCESS ->

                    paymentVerificationPersistenceService
                            .reconcilePaymentSuccess(
                                    payment.getProvider(),
                                    payment.getProviderOrderId(),
                                    providerResponse.getProviderPaymentId()
                            );


            case FAILED ->

                    paymentVerificationPersistenceService
                            .reconcilePaymentFailure(
                                    payment.getProvider(),
                                    payment.getProviderOrderId(),
                                    providerResponse.getProviderPaymentId()
                            );


            case PENDING ->

                    log.debug(
                            "Payment still pending at provider | paymentId={} | providerOrderId={}",
                            payment.getId(),
                            payment.getProviderOrderId()
                    );


            default ->

                    log.warn(
                            "Unknown reconciliation payment status | paymentId={} | status={}",
                            payment.getId(),
                            providerResponse.getStatus()
                    );
        }
    }
}