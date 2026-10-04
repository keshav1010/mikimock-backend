package MikiMock.com.MikiMock.Payment.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class CreateGatewayOrderRequest {

    private final String internalPaymentId;

    private final BigDecimal amount;

    private final String currency;

}