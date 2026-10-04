package MikiMock.com.MikiMock.Payment.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CreateGatewayOrderResponse {

    private final String gatewayOrderId;

    private final String publicKey;
}