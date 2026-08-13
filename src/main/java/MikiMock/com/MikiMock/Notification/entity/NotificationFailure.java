package MikiMock.com.MikiMock.Notification.entity;


import MikiMock.com.MikiMock.Notification.dto.NotificationType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "notification_failures",
        indexes = {

                @Index(name = "idx_event_id", columnList = "eventId"),

                @Index(name = "idx_status", columnList = "status"),

                @Index(name = "idx_created_at", columnList = "createdAt")

        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationFailure {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String eventId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationType notificationType;

    @Column(nullable = false)
    private String toEmail;

    private String subject;

    @Lob
    private String body;

    /**
     * Kafka Details
     */
    private String topic;

    private Integer partitionNo;

    private Long offsetNo;

    /**
     * Retry Details
     */
    private Integer retryCount;

    /**
     * Exception Details
     */
    private String exceptionClass;

    @Column(length = 2000)
    private String exceptionMessage;

    @Lob
    private String stackTrace;

    /**
     * Original Kafka Payload
     */
    @Lob
    private String originalPayload;

    @Enumerated(EnumType.STRING)
    private NotificationFailureStatus status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {

        createdAt = LocalDateTime.now();

        updatedAt = LocalDateTime.now();

    }

    @PreUpdate
    public void preUpdate() {

        updatedAt = LocalDateTime.now();

    }

}