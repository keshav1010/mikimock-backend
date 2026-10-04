package MikiMock.com.MikiMock.Payment.service;

import MikiMock.com.MikiMock.Payment.dto.CreatePaymentRequest;
import MikiMock.com.MikiMock.Payment.dto.CreatePaymentResponse;
import MikiMock.com.MikiMock.Payment.dto.VerifyPaymentRequest;
import MikiMock.com.MikiMock.Payment.dto.VerifyPaymentResponse;
import org.springframework.stereotype.Service;

@Service
public interface PaymentService {
    CreatePaymentResponse createPayment(CreatePaymentRequest request);

    VerifyPaymentResponse verifyPayment(VerifyPaymentRequest request);
}
