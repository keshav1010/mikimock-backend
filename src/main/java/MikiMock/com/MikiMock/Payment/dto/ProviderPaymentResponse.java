package MikiMock.com.MikiMock.Payment.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ProviderPaymentResponse {

    private final String providerOrderId;

    private final String providerPaymentId;

    private final String checkoutToken;

    private final String checkoutUrl;

    private final String providerName;
}
