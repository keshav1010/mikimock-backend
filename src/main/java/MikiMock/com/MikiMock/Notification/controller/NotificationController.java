//package MickiMock.example.MickiMock.Notification.controller;
//
//import MickiMock.example.MickiMock.Common.Response.ResponseUtil;
//import MickiMock.example.MickiMock.Notification.dto.NotificationEvent;
//import MickiMock.example.MickiMock.Notification.producer.NotificationProducer;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.PostMapping;
//import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.bind.annotation.RestController;
//
//@RestController
//@Slf4j
//@RequiredArgsConstructor
//@RequestMapping("/notification")
//public class NotificationController {
//    private final NotificationProducer notificationProducer;
//
//    @PostMapping("/SendMailDemo")
//    public ResponseEntity<String> demo(){
//        log.info("SendMailDemo hits");
//        NotificationEvent event = NotificationEvent.builder()
//                .toEmail("keshav.dogra.0022@gmail.com")
//                .subject("Message from kafka")
//                .body("This is a Demo message from kafka")
//                .build();
//
//        notificationProducer.sendNotification(event);
//
//        return ResponseEntity.ok("Successful sending email");
//    }
//
//}
