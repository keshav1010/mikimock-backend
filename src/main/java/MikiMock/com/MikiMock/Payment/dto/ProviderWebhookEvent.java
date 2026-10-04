package MikiMock.com.MikiMock.Payment.dto;

import MikiMock.com.MikiMock.Payment.entity.PaymentProviderType;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ProviderWebhookEvent {

    private PaymentProviderType provider;

    private String eventType;

    private String providerOrderId;

    private String providerPaymentId;

    private String providerPaymentStatus;

    private String rawPayload;
}
