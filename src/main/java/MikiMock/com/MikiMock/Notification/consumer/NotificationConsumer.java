//package MickiMock.example.MickiMock.Notification.consumer;
//
//import MickiMock.example.MickiMock.Notification.dto.NotificationEvent;
//import MickiMock.example.MickiMock.Notification.service.EmailServices;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.kafka.annotation.KafkaListener;
//import org.springframework.stereotype.Service;
//import tools.jackson.databind.ObjectMapper;
//
//
//@Service
//@Slf4j
//@RequiredArgsConstructor
//public class NotificationConsumer {
//    private final EmailServices emailServices;
//    private final ObjectMapper objectMapper;
//
//    @KafkaListener(
//            topics ="Notification-Topic",
//            groupId ="notification-group"
//    )
//
//    public void consume(String message){
//        try {
//            NotificationEvent event = objectMapper.readValue(message , NotificationEvent.class);
//            log.info("Notification Consumed");
//            emailServices.sendMail(event.getToEmail(), event.getSubject(), event.getBody());
//        } catch (Exception e) {
//            log.info("Error in consuming notification");
//            throw new RuntimeException(e);
//        }
//    }
//}
