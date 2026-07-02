package MikiMock.com.MikiMock.Collab.controller;


import MikiMock.com.MikiMock.Collab.dto.CollabAuthorizeResponse;
import MikiMock.com.MikiMock.Collab.service.CollabAuthorizeService;
import MikiMock.com.MikiMock.Common.Response.ApiResponse;
import MikiMock.com.MikiMock.Common.Response.ResponseUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/collab")
@RequiredArgsConstructor
public class CollabAuthorizeController {

    private final CollabAuthorizeService
            collabAuthorizeService;

    @GetMapping("/rooms/{roomCode}/authorize")
    public ResponseEntity<ApiResponse<CollabAuthorizeResponse>>
    authorize(
            @PathVariable String roomCode
    ) {

        return ResponseUtil.success(
                "Collab access verified",
                collabAuthorizeService.authorize(roomCode)
        );
    }
}