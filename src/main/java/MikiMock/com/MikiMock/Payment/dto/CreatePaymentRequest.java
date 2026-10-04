package MikiMock.com.MikiMock.Payment.dto;

import MikiMock.com.MikiMock.Payment.entity.PaymentProviderType;
import MikiMock.com.MikiMock.Payment.entity.SubscriptionPlanCode;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.Currency;

@Getter
@Setter
public class CreatePaymentRequest {

    @NotNull(message = "Subscription plan is required")
    private SubscriptionPlanCode planCode;

    @NotNull(message = "Payment provider is required")
    private PaymentProviderType providerType;

    private String couponCode;
}