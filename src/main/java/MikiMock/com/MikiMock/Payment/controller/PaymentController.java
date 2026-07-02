package MikiMock.com.MikiMock.Payment.controller;


import MikiMock.com.MikiMock.Common.Response.ApiResponse;
import MikiMock.com.MikiMock.Common.Response.ResponseUtil;
import MikiMock.com.MikiMock.Payment.dto.CreateOrderRequest;
import MikiMock.com.MikiMock.Payment.dto.CreateOrderResponse;
import MikiMock.com.MikiMock.Payment.dto.VerifyPaymentRequest;
import MikiMock.com.MikiMock.Payment.service.OrderService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@Slf4j
@AllArgsConstructor
@RequestMapping("api/v1/payments")
public class PaymentController {
    private final OrderService orderService;


    @PostMapping("/order-create")
    public ResponseEntity<ApiResponse<CreateOrderResponse>> createOrder(@Valid @RequestBody CreateOrderRequest request){
        String correlationId = MDC.get("X-Correlation-Id");
        log.info("Order creation request | correlation={}",correlationId);

        CreateOrderResponse response = orderService.createOrder(request);

        return ResponseUtil.created("Order creation successful",response);

    }
    @PostMapping("/verify")
    public ResponseEntity<
            ApiResponse<String>
            > verifyPayment(

            @Valid
            @RequestBody
            VerifyPaymentRequest request
    ) {

        String correlationId = MDC.get("X-Correlation-Id");
        log.info("Payment verification request | correlation={}",correlationId);

        orderService.verifyPayment(request);

        log.info("Payment verification successful | correlation={}",correlationId);

        return ResponseUtil.success(
                "Payment verified successfully",
                "PREMIUM ACTIVATED"
        );


    }

}
