package MikiMock.com.MikiMock.Notification.service;

import MikiMock.com.MikiMock.Notification.dto.ReplayResponse;

public interface NotificationReplayService {

    ReplayResponse replay(String eventId);

}