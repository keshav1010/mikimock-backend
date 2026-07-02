//package MickiMock.example.MickiMock.Notification.producer;
//
//
//import MickiMock.example.MickiMock.Notification.dto.NotificationEvent;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.apache.kafka.clients.producer.KafkaProducer;
//import org.springframework.kafka.core.KafkaTemplate;
//import org.springframework.stereotype.Service;
//import tools.jackson.core.JacksonException;
//import tools.jackson.databind.ObjectMapper;
//
//@Service
//@Slf4j
//@RequiredArgsConstructor
//public class NotificationProducer {
//
//    private final KafkaTemplate<String, String> kafkaTemplate;                          //datatype in kafka
//    private final ObjectMapper objectMapper;                                            //convert in Json
//    private static final String topic = "Notification-Topic";
//
//    public void sendNotification(NotificationEvent event){
//        try {
//            String message = objectMapper.writeValueAsString(event);
//
//            kafkaTemplate.send(topic ,message);
//            log.info("Notification published in kafka");
//        } catch (JacksonException e) {
//            throw new RuntimeException(e);
//        }
//    }
//}
