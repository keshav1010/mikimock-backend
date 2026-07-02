package MikiMock.com.MikiMock.Payment.service;

import MikiMock.com.MikiMock.Common.Exception.BusinessException;
import MikiMock.com.MikiMock.Payment.dto.CreateOrderRequest;
import MikiMock.com.MikiMock.Payment.dto.CreateOrderResponse;
import MikiMock.com.MikiMock.Payment.dto.VerifyPaymentRequest;
import MikiMock.com.MikiMock.Payment.entity.Payment;
import MikiMock.com.MikiMock.Payment.entity.PaymentStatus;
import MikiMock.com.MikiMock.Payment.repository.PaymentRespositoy;
import MikiMock.com.MikiMock.User.entity.SubscriptionType;
import MikiMock.com.MikiMock.User.entity.User;
import MikiMock.com.MikiMock.User.repository.UserRepository;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.Utils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImp implements OrderService{
    private final PaymentRespositoy paymentRespositoy;
    private final UserRepository userRepository;
    private final RazorpayClient razorpayClient;

    @Value("${razorpay.key-id}")
    private String razorpayKey;

    @Value("${razorpay.key-secret}")
    String razorpaySecret;

    @Override
    public CreateOrderResponse createOrder(CreateOrderRequest request) {
        try {
            User user = getAuthenticatedUser();

            if (user.getSubscriptionType() == SubscriptionType.PREMIUM){
                throw new BusinessException("User already has premium subscription");
            }

            JSONObject orderRequest = new JSONObject();
            orderRequest.put("amount",request.getAmount());
            orderRequest.put("currency",request.getCurrency());

//            orderRequest.put("paymentId",request.)
//            orderRequest.put("receipt_", "rcpt_"+UUID.randomUUID());

            Order razorpayOrder = razorpayClient.orders.create(orderRequest);

            Payment payment = Payment.builder()
                    .user(user)
                    .amount(request.getAmount())
                    .status(PaymentStatus.PENDING)
                    .orderId(razorpayOrder.get("id").toString())
//                    .signature(razorpayOrder.get("signature").toString())
                    .build();

            Payment savedPayment = paymentRespositoy.save(payment);

            return CreateOrderResponse.builder()

                    .paymentId(
                            savedPayment.getPaymentId()
                    )

                    .razorpayOrderId(
                            savedPayment
                                    .getOrderId()
                    )

                    .razorpayKey(
                            razorpayKey
                    )

                    .amount(
                            savedPayment.getAmount()
                    )

                    .currency(
                            request.getCurrency()
                    )

                    .status(
                            savedPayment
                                    .getStatus()
                                    .name()
                    )

                    .build();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }



    @Override
    @Transactional
    public void verifyPayment(
            VerifyPaymentRequest request
    ) {



        Payment payment = paymentRespositoy.findByPaymentId(request.getPaymentId());
        if(payment == null) throw new BusinessException("Payment not found");
        log.info("request={}",request);
        if (payment.getStatus()
                == PaymentStatus.SUCCESS) {

            throw new BusinessException(
                    "Payment already verified"
            );
        }

        // =========================
        // TODO:
        // Razorpay Signature Verification
        // =========================
        boolean isValid;
        try {
            JSONObject options =
                new JSONObject();

            options.put(
                    "razorpay_order_id",
                    request.getOrderId()
            );

            options.put(
                    "razorpay_payment_id",
                    request.getTransactionId()
            );

            options.put(
                    "razorpay_signature",
                    request.getSignature()
            );


            log.info("Options = {}",options);

            isValid =Utils.verifyPaymentSignature(
                                options,
                                razorpaySecret
                        );

        } catch (Exception e) {

            throw new BusinessException(
                    "Payment verification failed"
            );
        }

        if (!isValid) {

            payment.setStatus(
                    PaymentStatus.FAILED
            );

            paymentRespositoy.save(payment);

            throw new BusinessException(
                    "Invalid payment signature"
            );
        };

        payment.setOrderId(
                request.getOrderId()
        );

        payment.setPaymentId(
                request.getPaymentId()
        );

        payment.setSignature(
                request.getSignature()
        );

        payment.setStatus(
                PaymentStatus.SUCCESS
        );

        User user = payment.getUser();

        user.setSubscriptionType(
                SubscriptionType.PREMIUM
        );

        userRepository.save(user);

        paymentRespositoy.save(payment);
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
