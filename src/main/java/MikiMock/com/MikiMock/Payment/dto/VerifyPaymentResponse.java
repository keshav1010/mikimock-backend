package MikiMock.com.MikiMock.Payment.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class VerifyPaymentResponse {

    private Long paymentId;

    private String status;

    private String message;

    private boolean alreadyProcessed;
}
