package MikiMock.com.MikiMock.Payment.reconciliation;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.scheduling.annotation.Scheduled;

import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentReconciliationScheduler {

    private final PaymentReconciliationService paymentReconciliationService;


    /*
     * Run every 5 minutes.
     */
    @Scheduled(fixedDelay = 300000)
    public void reconcilePayments() {
        log.debug("Payment reconciliation scheduler started");

        paymentReconciliationService.reconcilePendingPayments();
    }
}