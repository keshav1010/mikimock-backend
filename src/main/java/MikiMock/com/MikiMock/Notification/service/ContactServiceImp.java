package MikiMock.com.MikiMock.Notification.service;

import MikiMock.com.MikiMock.Common.Exception.BusinessException;
import MikiMock.com.MikiMock.Notification.dto.ContactRequest;
import MikiMock.com.MikiMock.Notification.dto.NotificationEvent;
import MikiMock.com.MikiMock.Notification.dto.NotificationType;
import MikiMock.com.MikiMock.Notification.producer.NotificationProducer;
import MikiMock.com.MikiMock.User.entity.User;
import MikiMock.com.MikiMock.User.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
@Slf4j
public class ContactServiceImp implements ContactService {

    private final UserRepository userRepository;
    private final NotificationProducer notificationProducer;

    @Value("${spring.ses.to-support-email}")
    private String toSupportEmail;

    @Override
    public void sendMessage(ContactRequest request) {

        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();

        User user = null;

        if (isAuthenticated(authentication)) {
            user = getAuthenticatedUser(authentication);
        }

        // Guest user
        if (user == null) {

            if (request.getEmail() == null
                    || request.getEmail().isBlank()) {

                throw new BusinessException(
                        "Email is required for guest users"
                );
            }
        }

        String email;
        String userName;
        String userId;

        if (user != null) {

            // NEVER trust email coming from frontend
            email = user.getEmail();
            userName = user.getFullName();
            userId = String.valueOf(user.getId());

        } else {

            // Guest email comes from request
            email = request.getEmail().trim();
            userName = "Guest User";
            userId = "Guest";
        }

        String subject = request.getSubject();

        String body = """
                %s

                User Email : %s
                User Name  : %s
                User Id    : %s
                """
                .formatted(
                        request.getBody(),
                        email,
                        userName,
                        userId
                );

        NotificationEvent event =
                NotificationEvent.builder()
                        .notificationType(
                                NotificationType.CONTACT_MESSAGE
                        )
                        .toEmail(toSupportEmail)
                        .subject(subject)
                        .body(body)
                        .build();

        notificationProducer.publish(event);
    }

    private boolean isAuthenticated(
            Authentication authentication
    ) {
        return authentication != null
                && authentication.isAuthenticated()
                && !(authentication
                instanceof AnonymousAuthenticationToken);
    }

    private User getAuthenticatedUser(
            Authentication authentication
    ) {

        String email = authentication.getName();

        return userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new BusinessException("User not found")
                );
    }
}

//
//@Service
//@RequiredArgsConstructor
//@Slf4j
//public class ContactServiceImp implements ContactService{
//
//    private final UserRepository userRepository;
//    private final NotificationProducer notificationProducer;
//
//    @Value("${spring.ses.to-support-email}")
//    private String toSupportEmail;
//
//    @Override
//    public void sendMessage(ContactRequest request) {
//        User user = getAuthenticatedUser();
//        String subject = request.getSubject();
//
//        String body = """
//        %s
//
//        User Email : %s
//        User Name  : %s
//        User Id    : %s
//        """
//                .formatted(
//                        request.getBody(),
//                        user.getEmail(),
//                        user.getFullName(),
//                        user.getId()
//                );
//
//
//
//
//        NotificationEvent event =
//                NotificationEvent.builder()
//
//                        .notificationType(
//                                NotificationType.CONTACT_MESSAGE
//                        )
//
//                        .toEmail(toSupportEmail)
//
//                        .subject(subject)
//
//                        .body(body)
//
//                        .build();
//
//        notificationProducer.publish(event);
//
//    }
//
//    private User getAuthenticatedUser(){
//        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
//
//        if(authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken){
//            log.warn("Unauthorized access attempt | correlationId={}", MDC.get("X-Correlation-Id"));
//            throw new BusinessException("User not authenticated");
//        }
//        String email = authentication.getName();
//
//        User user = userRepository.findByEmail(email)
//                .orElseThrow(() -> {
//                    throw new BusinessException("User not found");
//                });
//        return user;
//    }
//}
