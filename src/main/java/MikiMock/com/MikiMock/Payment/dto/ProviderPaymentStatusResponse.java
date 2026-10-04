package MikiMock.com.MikiMock.Payment.dto;

import MikiMock.com.MikiMock.Payment.entity.PaymentStatus;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ProviderPaymentStatusResponse {

    private final String providerOrderId;

    private final String providerPaymentId;

    private final PaymentStatus status;
}