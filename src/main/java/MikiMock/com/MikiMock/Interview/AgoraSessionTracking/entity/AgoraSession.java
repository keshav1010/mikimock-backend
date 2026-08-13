package MikiMock.com.MikiMock.Interview.AgoraSessionTracking.entity;


import MikiMock.com.MikiMock.Interview.entity.Room;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "agora_session",
        indexes = {

                @Index(
                        name = "idx_agora_room",
                        columnList = "room_id"
                ),

                @Index(
                        name = "idx_agora_status",
                        columnList = "status"
                ),

                @Index(
                        name = "idx_agora_started_at",
                        columnList = "started_at"
                )
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgoraSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * One Interview Room
     * -> One Agora Session
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "room_id",
            nullable = false,
            unique = true
    )
    private Room room;

    /**
     * Agora Channel Name
     */
    @Column(
            name = "channel_name",
            nullable = false,
            length = 100
    )
    private String channelName;

    /**
     * ACTIVE / COMPLETED
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AgoraSessionStatus status;

    /**
     * NORMAL
     * ADMIN
     * SCHEDULER
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "closed_by")
    private AgoraSessionClosedBy closedBy;

    /**
     * Session Start
     */
    @Column(
            name = "started_at",
            nullable = false
    )
    private LocalDateTime startedAt;

    /**
     * Session End
     */
    @Column(name = "ended_at")
    private LocalDateTime endedAt;

    /**
     * Total RTC Minutes
     *
     * We will calculate
     * when session closes.
     */
    @Column(name = "participant_minutes")
    private Long participantMinutes;

    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private LocalDateTime createdAt;

    @Column(
            name = "updated_at",
            nullable = false
    )
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {

        createdAt = LocalDateTime.now();

        updatedAt = LocalDateTime.now();

        status = AgoraSessionStatus.ACTIVE;
    }

    @PreUpdate
    public void preUpdate() {

        updatedAt = LocalDateTime.now();
    }
}
