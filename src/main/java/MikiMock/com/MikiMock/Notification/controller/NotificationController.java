package MikiMock.com.MikiMock.Notification.controller;


import MikiMock.com.MikiMock.Common.Response.ApiResponse;
import MikiMock.com.MikiMock.Common.Response.ResponseUtil;
import MikiMock.com.MikiMock.Notification.dto.ContactRequest;
import MikiMock.com.MikiMock.Notification.dto.NotificationEvent;
import MikiMock.com.MikiMock.Notification.producer.NotificationProducer;
import MikiMock.com.MikiMock.Notification.service.ContactService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/api/v1/contact")
public class NotificationController {
    private final ContactService contactService;

    @PostMapping("/message")
    public ResponseEntity<ApiResponse<String>> sendMessage(@Valid @RequestBody ContactRequest request){
        log.info("contact message called");
        contactService.sendMessage(request);

        return ResponseUtil.success("Successful sending email" , "Successful sending email");
    }

}
