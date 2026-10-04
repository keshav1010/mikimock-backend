package MikiMock.com.MikiMock.Payment.gateway;

import MikiMock.com.MikiMock.Common.Exception.BusinessException;
import MikiMock.com.MikiMock.Payment.dto.ProviderPaymentRequest;
import MikiMock.com.MikiMock.Payment.dto.ProviderPaymentResponse;
import MikiMock.com.MikiMock.Payment.dto.ProviderPaymentStatusResponse;
import MikiMock.com.MikiMock.Payment.dto.ProviderWebhookEvent;
import MikiMock.com.MikiMock.Payment.entity.PaymentProviderType;
import MikiMock.com.MikiMock.Payment.entity.PaymentStatus;
import com.razorpay.Order;
import com.razorpay.Payment;
import com.razorpay.RazorpayClient;
import com.razorpay.Utils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.List;


@Slf4j
@Component
@RequiredArgsConstructor
public class RazorpayPaymentProvider implements PaymentGateway{

    private final RazorpayClient razorpayClient;
    private final ObjectMapper objectMapper;

    @Value("${razorpay.key-id}")
    private String razorpayKeyId;

    @Value("${razorpay.key-secret}")
    private String razorpayKeySecret;

    @Value("${razorpay.webhook-secret}")
    private String webhookSecret;

    @Override
    public PaymentProviderType getProviderType() {
        return PaymentProviderType.RAZORPAY;
    }

    @Override
    public boolean verifyWebhookSignature(
            String rawPayload,
            String signature
    ) {

        try {

            return Utils.verifyWebhookSignature(
                    rawPayload,
                    signature,
                    webhookSecret
            );

        } catch (Exception exception) {

            log.error(
                    "Razorpay webhook signature verification failed",
                    exception
            );

            return false;
        }
    }

    @Override
    public ProviderPaymentResponse createPayment(ProviderPaymentRequest request) {
        try {
            /*
             * Razorpay expects the amount
             * in the smallest currency unit.
             *
             * INR:
             * 499.00 -> 49900 paisa
             */
            long amountInSmallestUnit = request.getAmount()
                            .multiply(BigDecimal.valueOf(100))
                            .longValueExact();

            JSONObject orderRequest = new JSONObject();

            orderRequest.put("amount", amountInSmallestUnit);

            orderRequest.put("currency", request.getCurrency().getCurrencyCode());

            orderRequest.put("receipt", request.getPaymentReference());

            Order order = razorpayClient.orders.create(orderRequest);


            String providerOrderId = order.get("id");

            log.info(
                    "Razorpay order created | paymentReference={} | providerOrderId={}",
                    request.getPaymentReference(),
                    providerOrderId
            );

            return ProviderPaymentResponse.builder()
                    .providerOrderId(providerOrderId)
                    .providerPaymentId(null)
                    /*
                     * Frontend needs this
                     * to initialize Razorpay checkout.
                     */
                    .checkoutToken(razorpayKeyId)
                    .checkoutUrl(null)
                    .providerName(PaymentProviderType.RAZORPAY.name())
                    .build();
        } catch (Exception exception) {
            log.error(
                    "Razorpay order creation failed | paymentReference={}",
                    request.getPaymentReference(),
                    exception
            );

            throw new BusinessException(
                    "Unable to create payment order"
            );
        }
    }

    @Override
    public boolean verifyPayment(String providerOrderId, String providerPaymentId, String signature) {
        try {

            JSONObject options =
                    new JSONObject();

            options.put(
                    "razorpay_order_id",
                    providerOrderId
            );

            options.put(
                    "razorpay_payment_id",
                    providerPaymentId
            );

            options.put(
                    "razorpay_signature",
                    signature
            );

            boolean valid =
                    Utils.verifyPaymentSignature(
                            options,
                            razorpayKeySecret
                    );
            if (!valid) {

                throw new BusinessException(
                        "Invalid Razorpay payment signature"
                );
            }

            log.info(
                    "Razorpay payment signature verified | providerOrderId={} | providerPaymentId={}",
                    providerOrderId,
                    providerPaymentId
            );

            return true;

        } catch (BusinessException exception) {

            throw exception;

        } catch (Exception exception) {

            log.error(
                    "Razorpay verification failed | providerOrderId={}",
                    providerOrderId,
                    exception
            );

            throw new BusinessException(
                    "Payment verification failed"
            );
        }
    }

    @Override
    public ProviderWebhookEvent parseWebhookEvent(
            String rawPayload
    ) {

        try {

            JsonNode root =
                    objectMapper.readTree(
                            rawPayload
                    );

            String eventType =
                    root.path("event")
                            .asText();

            JsonNode paymentEntity =
                    root.path("payload")
                            .path("payment")
                            .path("entity");

            String providerOrderId =
                    paymentEntity
                            .path("order_id")
                            .asText(null);

            String providerPaymentId =
                    paymentEntity
                            .path("id")
                            .asText(null);

            String providerPaymentStatus =
                    paymentEntity
                            .path("status")
                            .asText(null);


            if (
                    providerOrderId == null ||
                            providerOrderId.isBlank()
            ) {

                throw new BusinessException(
                        "Invalid Razorpay webhook: order ID missing"
                );
            }


            return ProviderWebhookEvent.builder()

                    .provider(
                            PaymentProviderType.RAZORPAY
                    )

                    .eventType(
                            eventType
                    )

                    .providerOrderId(
                            providerOrderId
                    )

                    .providerPaymentId(
                            providerPaymentId
                    )

                    .providerPaymentStatus(
                            providerPaymentStatus
                    )

                    .rawPayload(
                            rawPayload
                    )

                    .build();


        } catch (BusinessException exception) {

            throw exception;

        } catch (Exception exception) {

            log.error(
                    "Failed to parse Razorpay webhook",
                    exception
            );

            throw new BusinessException(
                    "Invalid Razorpay webhook payload"
            );
        }
    }

    @Override
    public ProviderPaymentStatusResponse getPaymentStatus(
            String providerOrderId
    ) {

        try {

            List<com.razorpay.Payment> payments =
                    razorpayClient
                            .orders
                            .fetchPayments(providerOrderId);


            /*
             * No payment attempt has been returned
             * for this order yet.
             */
            if (payments == null || payments.isEmpty()) {

                return ProviderPaymentStatusResponse
                        .builder()
                        .providerOrderId(providerOrderId)
                        .providerPaymentId(null)
                        .status(PaymentStatus.PENDING)
                        .build();
            }


            /*
             * An order can have multiple payment attempts.
             *
             * Example:
             *
             * pay_001 -> failed
             * pay_002 -> failed
             * pay_003 -> captured
             *
             * We are interested in the captured payment.
             */
            for (com.razorpay.Payment payment : payments) {

                String razorpayStatus =
                        payment.get("status").toString();


                if ("captured".equalsIgnoreCase(razorpayStatus)) {

                    String providerPaymentId =
                            payment.get("id").toString();


                    log.info(
                            "Razorpay payment captured | providerOrderId={} | providerPaymentId={}",
                            providerOrderId,
                            providerPaymentId
                    );


                    return ProviderPaymentStatusResponse
                            .builder()
                            .providerOrderId(providerOrderId)
                            .providerPaymentId(providerPaymentId)
                            .status(PaymentStatus.SUCCESS)
                            .build();
                }
            }


            /*
             * No captured payment was found.
             *
             * Check whether all available attempts have failed.
             */
            boolean hasFailedPayment =
                    payments.stream()
                            .anyMatch(payment ->
                                    "failed".equalsIgnoreCase(
                                            payment.get("status").toString()
                                    )
                            );


            boolean hasPendingPayment =
                    payments.stream()
                            .anyMatch(payment ->
                                    !"failed".equalsIgnoreCase(
                                            payment.get("status").toString()
                                    )
                            );


            /*
             * If there is still an attempt that may complete,
             * keep the MikiMock payment pending.
             */
            if (hasPendingPayment) {

                return ProviderPaymentStatusResponse
                        .builder()
                        .providerOrderId(providerOrderId)
                        .providerPaymentId(null)
                        .status(PaymentStatus.PENDING)
                        .build();
            }


            /*
             * All returned payment attempts are failed
             * and none was captured.
             */
            if (hasFailedPayment) {

                String latestPaymentId =
                        payments.get(
                                payments.size() - 1
                        ).get("id").toString();


                return ProviderPaymentStatusResponse
                        .builder()
                        .providerOrderId(providerOrderId)
                        .providerPaymentId(latestPaymentId)
                        .status(PaymentStatus.FAILED)
                        .build();
            }


            /*
             * Defensive fallback.
             */
            return ProviderPaymentStatusResponse
                    .builder()
                    .providerOrderId(providerOrderId)
                    .providerPaymentId(null)
                    .status(PaymentStatus.PENDING)
                    .build();


        } catch (Exception exception) {

            log.error(
                    "Failed to fetch Razorpay payment status | providerOrderId={}",
                    providerOrderId,
                    exception
            );

            throw new BusinessException(
                    "Failed to fetch payment status from provider"
            );
        }
    }

//    @Override
//    public ProviderPaymentStatusResponse getPaymentStatus(
//            String providerOrderId
//    ) {
//
//        try {
//
//            List<com.razorpay.Payment> payments =
//                    razorpayClient
//                            .orders
//                            .fetchPayments(
//                                    providerOrderId
//                            );
//
//            if (payments.isEmpty()) {
//
//                return ProviderPaymentStatusResponse
//                        .builder()
//                        .providerOrderId(providerOrderId)
//                        .providerPaymentId(null)
//                        .status(PaymentStatus.PENDING)
//                        .build();
//            }
//
//
//            /*
//             * Get the latest provider payment.
//             */
//            Payment latestPayment =
//                    payments.get(
//                            payments.size() - 1
//                    );
//
//
//            String razorpayStatus =
//                    latestPayment.get("status")
//                            .toString();
//
//
//            String providerPaymentId =
//                    latestPayment.get("id")
//                            .toString();
//
//
//            PaymentStatus paymentStatus =
//                    switch (razorpayStatus) {
//
//                        case "captured" ->
//                                PaymentStatus.SUCCESS;
//
//                        case "failed" ->
//                                PaymentStatus.FAILED;
//
//                        default ->
//                                PaymentStatus.PENDING;
//                    };
//
//
//            return ProviderPaymentStatusResponse
//                    .builder()
//                    .providerOrderId(providerOrderId)
//                    .providerPaymentId(providerPaymentId)
//                    .status(paymentStatus)
//                    .build();
//
//        } catch (Exception e) {
//
//            log.error(
//                    "Failed to fetch Razorpay payment status | providerOrderId={}",
//                    providerOrderId,
//                    e
//            );
//
//            throw new BusinessException(
//                    "Failed to fetch payment status from provider"
//            );
//        }
//    }
}
