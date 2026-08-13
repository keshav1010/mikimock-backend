package MikiMock.com.MikiMock.Notification.cache;

public interface NotificationIdempotencyService {
    boolean isProcessed(String eventId);

    void markProcessed(String eventId);
}
