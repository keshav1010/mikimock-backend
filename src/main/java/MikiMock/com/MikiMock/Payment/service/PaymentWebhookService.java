package MikiMock.com.MikiMock.Payment.service;

import MikiMock.com.MikiMock.Payment.entity.PaymentProviderType;
import org.springframework.stereotype.Service;

@Service
public interface PaymentWebhookService {

    void processWebhook(
            PaymentProviderType provider,
            String signature,
            String rawPayload
    );
}