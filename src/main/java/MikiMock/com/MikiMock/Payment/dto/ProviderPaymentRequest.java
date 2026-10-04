package MikiMock.com.MikiMock.Payment.dto;

import MikiMock.com.MikiMock.Payment.entity.PaymentProviderType;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.Currency;

@Getter
@Builder
public class ProviderPaymentRequest {

    private final String paymentReference;

    private final BigDecimal amount;

    private final Currency currency;

    private final String description;

    private final String customerEmail;

    private final PaymentProviderType providerType;
}