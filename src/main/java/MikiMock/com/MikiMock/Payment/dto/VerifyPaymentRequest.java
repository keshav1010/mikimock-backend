package MikiMock.com.MikiMock.Payment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class VerifyPaymentRequest {

//    @NotNull
//    private UUID paymentId;
//
//    @NotBlank
//    private String orderId;
//
//    @NotBlank
//    private String transactionId;
//
//    @NotBlank
//    private String paymentReference;
//
//    @NotBlank
//    private String signature;

    @NotNull(message = "Payment ID is required")
    private Long paymentId;

    @NotBlank(message = "Provider order ID is required")
    private String providerOrderId;

    @NotBlank(message = "Provider payment ID is required")
    private String providerPaymentId;

    @NotBlank(message = "Payment signature is required")
    private String signature;
}