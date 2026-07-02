package MikiMock.com.MikiMock.Payment.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class CreateOrderResponse {

    private UUID paymentId;

    private String razorpayOrderId;

    private String razorpayKey;

    private Double amount;

    private String currency;

    private String status;
}