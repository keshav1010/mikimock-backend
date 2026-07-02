package MikiMock.com.MikiMock.Interview.entity;


import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "rooms",
indexes = {
        @Index(name = "idx_room_public_id", columnList = "public_id"),
        @Index(name = "idx_room_status", columnList = "status"),
        @Index(name = "idx_room_start_time", columnList = "start_at")
        }
    )
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Room {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "public_id",
            nullable = false,
            unique = true,
            updatable = false
    )
    private UUID publicId;

    @Column(name = "room_code",
            nullable = false,
            unique = true,
            length = 200
    )
    private String roomCode;

    @Column(nullable = false, length = 20)
    private String topic;

    @Column(nullable = false,length = 10)
    private  String level;

    @Enumerated(EnumType.STRING)
    private RoomStatus status;

    @Column(name = "start_at")
    private LocalDateTime startAt;

    @Column(name = "ended_at")
    private LocalDateTime endedAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "created_at")
    @CreationTimestamp
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "room_mode", nullable = false)
    private RoomMode roomMode;


    @Column(name = "current_language")
    private String currentLanguage;

    @PrePersist
    public void prePersist() {

        if (this.publicId == null) {
            this.publicId = UUID.randomUUID();
        }
        if (this.roomMode == null) {
            this.roomMode = RoomMode.CODING_INTERVIEW;
        }
    }
}
