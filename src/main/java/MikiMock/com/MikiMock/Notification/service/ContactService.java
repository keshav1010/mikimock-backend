package MikiMock.com.MikiMock.Notification.service;

import MikiMock.com.MikiMock.Notification.dto.ContactRequest;
import org.springframework.stereotype.Service;


@Service
public interface ContactService {
    void sendMessage(ContactRequest request);
}
