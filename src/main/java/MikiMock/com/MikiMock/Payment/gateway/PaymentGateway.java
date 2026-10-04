package MikiMock.com.MikiMock.Payment.gateway;

import MikiMock.com.MikiMock.Payment.dto.ProviderPaymentRequest;
import MikiMock.com.MikiMock.Payment.dto.ProviderPaymentResponse;
import MikiMock.com.MikiMock.Payment.dto.ProviderPaymentStatusResponse;
import MikiMock.com.MikiMock.Payment.dto.ProviderWebhookEvent;
import MikiMock.com.MikiMock.Payment.entity.PaymentProviderType;

public interface PaymentGateway {

    PaymentProviderType getProviderType();

    ProviderPaymentResponse createPayment(ProviderPaymentRequest request);

    boolean verifyWebhookSignature(String rawPayload, String signature);

    public ProviderWebhookEvent parseWebhookEvent(String rawPayload);

    boolean verifyPayment(String providerOrderId, String providerPaymentId, String signature);

    ProviderPaymentStatusResponse getPaymentStatus(String providerOrderId);

}
