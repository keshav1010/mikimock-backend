package MikiMock.com.MikiMock.Payment.service;

import MikiMock.com.MikiMock.Payment.entity.Payment;
import MikiMock.com.MikiMock.Payment.repository.PaymentRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PaymentPersistenceServiceImpl implements PaymentPersistenceService {

    private final PaymentRepository paymentRepository;

    @Override
    @Transactional
    public Payment savePayment(Payment payment) {

        return paymentRepository.save(payment);
    }
}
