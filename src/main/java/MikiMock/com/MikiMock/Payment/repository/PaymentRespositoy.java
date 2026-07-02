package MikiMock.com.MikiMock.Payment.repository;

import MikiMock.com.MikiMock.Payment.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PaymentRespositoy extends JpaRepository<Payment, Long> {
    Payment findByPaymentId(UUID id);
}
