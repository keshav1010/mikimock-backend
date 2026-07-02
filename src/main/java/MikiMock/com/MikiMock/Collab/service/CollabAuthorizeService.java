package MikiMock.com.MikiMock.Collab.service;

import MikiMock.com.MikiMock.Collab.dto.CollabAuthorizeResponse;
import org.springframework.stereotype.Service;

@Service
public interface CollabAuthorizeService {
    CollabAuthorizeResponse authorize(String roomCode);
}
