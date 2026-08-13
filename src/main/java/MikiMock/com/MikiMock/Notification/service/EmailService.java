package MikiMock.com.MikiMock.Notification.service;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {


    private final SesClient sesClient;


    @Value("${spring.ses.from-email}")
    private String fromEmail;


    public void sendMail(String to,String subject,String body) {


        SendEmailRequest request = SendEmailRequest.builder()
                .source(fromEmail)
                .destination(Destination.builder().toAddresses(to).build())
                .message(Message.builder().subject(Content.builder().data(subject).build())
                        .body(Body.builder().text(Content.builder().data(body).build()).build())
                        .build())
                .build();


        try {

            SendEmailResponse response =
                    sesClient.sendEmail(request);


            log.info(
                    "Mail sent successfully to {} with messageId {}",
                    to,
                    response.messageId()
            );


        } catch (SesException e) {

            log.error(
                    "Failed to send mail to {}",
                    to,
                    e
            );

            throw e;
        }
    }
}

