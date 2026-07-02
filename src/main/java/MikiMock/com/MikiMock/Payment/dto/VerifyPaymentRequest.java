package MikiMock.com.MikiMock.Payment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class VerifyPaymentRequest {

    @NotNull
    private UUID paymentId;

    @NotBlank
    private String orderId;

    @NotBlank
    private String transactionId;

    @NotBlank
    private String paymentReference;

    @NotBlank
    private String signature;
}