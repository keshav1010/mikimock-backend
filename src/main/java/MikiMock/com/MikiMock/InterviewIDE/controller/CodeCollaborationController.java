package MikiMock.com.MikiMock.InterviewIDE.controller;

import MikiMock.com.MikiMock.InterviewIDE.dto.CodeUpdateRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class CodeCollaborationController {

    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/code.update")
    public void updateCode(
            CodeUpdateRequest request
    ) {

        messagingTemplate.convertAndSend(

                "/topic/room/" + request.getRoomCode(),

                request
        );
    }
}