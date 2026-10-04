package MikiMock.com.MikiMock.Payment.service;

import MikiMock.com.MikiMock.Common.Exception.BusinessException;
import MikiMock.com.MikiMock.Payment.dto.*;
import MikiMock.com.MikiMock.Payment.entity.Payment;
import MikiMock.com.MikiMock.Payment.entity.PaymentStatus;
import MikiMock.com.MikiMock.Payment.entity.SubscriptionPlan;
import MikiMock.com.MikiMock.Payment.factory.PaymentProviderFactory;
import MikiMock.com.MikiMock.Payment.gateway.PaymentGateway;
import MikiMock.com.MikiMock.Payment.repository.PaymentRepository;
import MikiMock.com.MikiMock.Payment.repository.SubscriptionPlanRepository;
import MikiMock.com.MikiMock.User.entity.SubscriptionType;
import MikiMock.com.MikiMock.User.entity.User;
import MikiMock.com.MikiMock.User.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Currency;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class PaymentServiceImp implements PaymentService{

    private final UserRepository userRepository;
    private final SubscriptionPlanRepository subscriptionPlanRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentProviderFactory paymentProviderFactory;
    private final PaymentPersistenceService paymentPersistenceService;
    private final PaymentVerificationPersistenceService paymentVerificationPersistenceService;

    @Override
    public CreatePaymentResponse createPayment(CreatePaymentRequest request) {

        User user = getAuthenticatedUser();

        if (user.getSubscriptionType() != SubscriptionType.FREE) {
            throw new BusinessException("User already have premium");
        }

        SubscriptionPlan subscriptionPlan = subscriptionPlanRepository.findByPlanCodeAndActiveTrue(request.getPlanCode())
                .orElseThrow(() -> new BusinessException("Subscription plan not found or inactive"));

        String paymentReference = "PAY_" + UUID.randomUUID().toString().replace("-", "").toUpperCase();
        Payment payment = Payment.builder()
                .amount(subscriptionPlan.getAmount())
                .currency(subscriptionPlan.getCurrency())
                .user(user)
                .paymentReference(paymentReference)
                .provider(request.getProviderType())
                .status(PaymentStatus.PENDING)
                .subscriptionPlan(subscriptionPlan)
                .build();

        payment = paymentPersistenceService.savePayment(payment);

        log.info("Internal payment created | paymentReference={} | userId={} | provider={} | amount={} {}",
                paymentReference, user.getId(), request.getProviderType(), payment.getAmount(), payment.getCurrency());

        PaymentGateway provider = paymentProviderFactory.getProvider(request.getProviderType());

        ProviderPaymentRequest providerRequest = ProviderPaymentRequest.builder()
                .paymentReference(paymentReference)
                .amount(subscriptionPlan.getAmount())
                .currency(Currency.getInstance(subscriptionPlan.getCurrency()))
                .description("MikiMock " + subscriptionPlan.getName())
                .customerEmail(user.getEmail())
                .providerType(request.getProviderType())
                .build();

        ProviderPaymentResponse providerResponse;

        try {
            providerResponse = provider.createPayment(providerRequest);
        } catch (Exception exception) {
            log.error("Provider payment initialization failed | paymentReference={}",
                    paymentReference, exception);

            payment.setStatus(PaymentStatus.FAILED);

            paymentPersistenceService.savePayment(payment);

            throw new BusinessException("Unable to initialize payment");
        }

        payment.setProviderOrderId(providerResponse.getProviderOrderId());

        paymentPersistenceService.savePayment(payment);

        log.info(
                "Payment initialized successfully | paymentReference={} | providerOrderId={} | provider={}",
                paymentReference,
                providerResponse.getProviderOrderId(),
                providerResponse.getProviderName()
        );

        return CreatePaymentResponse.builder()
                .paymentId(payment.getId())
                .paymentReference(paymentReference)
                .providerOrderId(providerResponse.getProviderOrderId())
                .checkoutToken(providerResponse.getCheckoutToken())
                .checkoutUrl(providerResponse.getCheckoutUrl())
                .provider(providerResponse.getProviderName())
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .status(payment.getStatus().name())
                .build();
    }

    @Override
    public VerifyPaymentResponse verifyPayment(VerifyPaymentRequest request) {
        /*
         * 1. Get current authenticated user.
         */
        User user = getAuthenticatedUser();
        /*
         * 2. Find payment.
         */

        log.info("getting payment for {}" , request.getPaymentId());
        Payment payment =paymentRepository.findById(request.getPaymentId()).orElseThrow( //findByIdForUpdate
                                () -> new BusinessException("Payment not found"));
        /*
         * 3. Security:
         * Payment must belong to current user.
         */
        if (!payment.getUser().getId().equals(user.getId())) {
            throw new BusinessException("You are not authorized to verify this payment");
        }
        /*
         * 4. Idempotency protection.
         */
        if (payment.getStatus() == PaymentStatus.SUCCESS) {
            log.info("Duplicate payment verification ignored | paymentId={}", payment.getId());
            return VerifyPaymentResponse.builder()
                    .paymentId(
                            payment.getId()
                    )
                    .status(
                            payment.getStatus().name()
                    )
                    .message(
                            "Payment was already verified"
                    )
                    .alreadyProcessed(true)
                    .build();
        }
        if (
                payment.getStatus()
                        == PaymentStatus.FAILED
        ) {

            throw new BusinessException(
                    "Payment verification has already failed"
            );
        }
        /*
         * 5. Verify frontend order ID matches
         * our original provider order.
         *
         * Never trust frontend data.
         */
        if (request.getProviderOrderId() != null && !request.getProviderOrderId().equals(payment.getProviderOrderId())) {
            log.warn("Provider order mismatch | paymentId={}", payment.getId());
            throw new BusinessException("Invalid payment order");
        }

        /*
         * 6. Get correct payment gateway.
         */

        log.info("Getting provider");

        PaymentGateway paymentGateway = paymentProviderFactory.getProvider(payment.getProvider());

        log.info("Payment Gateway {}" ,paymentGateway.getProviderType());

        /*
         * 7. Verify signature with provider.
         * No DB transaction is open here.
         */

        boolean valid = paymentGateway.verifyPayment(payment.getProviderOrderId(),
                        request.getProviderPaymentId(),
                        request.getSignature());
        /*
         * 8. Invalid payment.
         */

        log.info("Check It's valid {}", valid );

        if (!valid) {
            paymentVerificationPersistenceService.markPaymentFailed(payment);
            throw new BusinessException("Invalid payment signature");
        }

        /*
         * 9. Valid payment.
         *
         * Short transaction:
         * Payment → SUCCESS
         * Provider Payment ID saved
         * Signature saved
         * User → PREMIUM
         */

        paymentVerificationPersistenceService.markPaymentSuccessful(
                        payment,
                        request.getProviderPaymentId(),
                        request.getSignature()
                );
        log.info("return response of payment");
        return VerifyPaymentResponse.builder()
                .paymentId(
                        payment.getId()
                )
                .status(
                        payment.getStatus().name()
                )
                .message(
                        "Payment verified successfully"
                )
                .alreadyProcessed(false)
                .build();
    }


    private User getAuthenticatedUser() {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated() ||authentication instanceof AnonymousAuthenticationToken){
            throw new BusinessException("User not authorized");
        }

        String email = authentication.getName();
        return userRepository.findByEmail(email).orElseThrow(() -> new BusinessException("User not found"));
    }
}
