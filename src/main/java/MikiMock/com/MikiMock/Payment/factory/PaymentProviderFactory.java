package MikiMock.com.MikiMock.Payment.factory;

import MikiMock.com.MikiMock.Common.Exception.BusinessException;
import MikiMock.com.MikiMock.Payment.entity.PaymentProviderType;
import MikiMock.com.MikiMock.Payment.gateway.PaymentGateway;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class PaymentProviderFactory {

    private final Map<PaymentProviderType , PaymentGateway> providerMap;

    public PaymentProviderFactory(List<PaymentGateway> paymentGatewayList) {
        this.providerMap = new EnumMap<>(PaymentProviderType.class);

        for(PaymentGateway provider : paymentGatewayList){
            PaymentProviderType providerType = provider.getProviderType();
            if(providerMap.containsKey(providerType)){
                throw  new IllegalStateException("Duplicate payment provider registered: " + providerType);
            }
            providerMap.put(providerType,provider);
        }
    }

    public PaymentGateway getProvider(PaymentProviderType providerType){
        PaymentGateway provider = providerMap.get(providerType);

        if (provider == null) {
            log.error("Unsupported payment provider | provider={}", providerType);
            throw new BusinessException("Unsupported payment provider");
        }

        return provider;

    }
}
