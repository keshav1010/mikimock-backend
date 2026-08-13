package MikiMock.com.MikiMock.Notification.controller;

import MikiMock.com.MikiMock.Notification.dto.ReplayResponse;
import MikiMock.com.MikiMock.Notification.service.NotificationReplayService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/notifications")
@RequiredArgsConstructor
public class NotificationReplayController {

    private final NotificationReplayService replayService;

    @PostMapping("/replay/{eventId}")
    public ResponseEntity<ReplayResponse> replay(
            @PathVariable String eventId
    ) {

        return ResponseEntity.ok(
                replayService.replay(eventId)
        );
    }
}