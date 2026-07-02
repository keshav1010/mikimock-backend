package MikiMock.com.MikiMock.Room.controller;

import MikiMock.com.MikiMock.Common.Response.ApiResponse;
import MikiMock.com.MikiMock.Common.Response.ResponseUtil;
import MikiMock.com.MikiMock.Room.dto.RoomTimerResponse;
import MikiMock.com.MikiMock.Room.service.RoomPresenceService;
import MikiMock.com.MikiMock.Room.service.RoomTimerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/rooms")
@RequiredArgsConstructor
@Slf4j
public class RoomController {

    private final RoomPresenceService presenceService;
    private final RoomTimerService roomTimerService;

    @PostMapping("/{roomCode}/presence/join")
    public ResponseEntity<ApiResponse<String>> joinRoom(@PathVariable String roomCode) {
    log.info("Join Room API hit ");

        presenceService.joinRoom(roomCode);

        return ResponseUtil.success(
                "Presence updated",
                "USER_JOINED"
        );
    }

    @PostMapping("/{roomCode}/presence/leave")
    public ResponseEntity<ApiResponse<String>> leaveRoom(@PathVariable String roomCode) {
        log.info("Left Room API hit ");
        presenceService.leaveRoom(roomCode);

        return ResponseUtil.success(
                "Presence updated",
                "USER_LEFT"
        );
    }

    @GetMapping("/{roomCode}/timer")
    public ResponseEntity<ApiResponse<RoomTimerResponse>> getTimer(@PathVariable String roomCode) {
        log.info("Timer request is received");
        RoomTimerResponse response = roomTimerService.getTimer(roomCode);
        log.info("Timer request is successfully processed");
        return ResponseUtil.success("Timer fetched successfully", response);
    }
}
