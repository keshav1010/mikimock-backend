package MikiMock.com.MikiMock.Payment.gateway;

import MikiMock.com.MikiMock.Common.Exception.BusinessException;
import MikiMock.com.MikiMock.Payment.dto.ProviderPaymentRequest;
import MikiMock.com.MikiMock.Payment.dto.ProviderPaymentResponse;
import MikiMock.com.MikiMock.Payment.dto.ProviderPaymentStatusResponse;
import MikiMock.com.MikiMock.Payment.dto.ProviderWebhookEvent;
import MikiMock.com.MikiMock.Payment.entity.PaymentProviderType;
import org.springframework.stereotype.Component;


@Component
public class PaypalPaymentProvider implements PaymentGateway{
    @Override
    public PaymentProviderType getProviderType() {
        return PaymentProviderType.PAYPAL;
    }

    @Override
    public ProviderPaymentResponse createPayment(ProviderPaymentRequest request) {
        throw new BusinessException(
                "PayPal is not currently available"
        );
    }

    @Override
    public boolean verifyWebhookSignature(String rawPayload, String signature) {
        return false;
    }

    @Override
    public ProviderWebhookEvent parseWebhookEvent(String rawPayload) {
        return null;
    }

    @Override
    public boolean verifyPayment(String providerOrderId, String providerPaymentId, String signature) {
        throw new BusinessException(
                "PayPal is not currently available"
        );
    }

    @Override
    public ProviderPaymentStatusResponse getPaymentStatus(String providerOrderId) {
        return null;
    }
}
