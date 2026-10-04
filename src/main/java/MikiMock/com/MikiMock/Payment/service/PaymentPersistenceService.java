package MikiMock.com.MikiMock.Payment.service;

import MikiMock.com.MikiMock.Payment.entity.Payment;

public interface PaymentPersistenceService {

    Payment savePayment(Payment payment);

}