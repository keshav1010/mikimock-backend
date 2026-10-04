package MikiMock.com.MikiMock.Payment.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class CreatePaymentResponse {

    private final Long paymentId;

    private final String paymentReference;

    private final String providerOrderId;

    private final String checkoutToken;

    private final String checkoutUrl;

    private final String provider;

    private final BigDecimal amount;

    private final String currency;

    private final String status;
}