package MikiMock.com.MikiMock.Payment.service;

import MikiMock.com.MikiMock.Payment.dto.CreateOrderRequest;
import MikiMock.com.MikiMock.Payment.dto.CreateOrderResponse;
import MikiMock.com.MikiMock.Payment.dto.VerifyPaymentRequest;
import org.springframework.stereotype.Service;

@Service
public interface OrderService {
    CreateOrderResponse createOrder(CreateOrderRequest request);
    public void verifyPayment(
            VerifyPaymentRequest request
    );
}
