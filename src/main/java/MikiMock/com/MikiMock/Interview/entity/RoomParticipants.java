package MikiMock.com.MikiMock.Interview.entity;


import MikiMock.com.MikiMock.User.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Table(name = "room_participants",
        uniqueConstraints = {
        @UniqueConstraint(
                name = "uk_room_participant_room_user",
                columnNames = {"room_id","user_id"}
        )
        },
        indexes = {
                @Index(name = "idx_room_participant_room", columnList = "room_id"),
                @Index(name = "idx_room_participant_user", columnList = "user_id"),
                @Index(name = "idx_room_participant_joinedAt", columnList = "joined_at")
        }
)

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor

public class RoomParticipants {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "room_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_room_participant_room")
    )
    private Room room;

    @ManyToOne(fetch = FetchType.LAZY,optional = false)
    @JoinColumn(
            name = "user_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_room_participant_user")
    )
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "participant_role")
    private ParticipantRole participantRole;

    @Column(name = "joined_at")
    private LocalDateTime joinedAt;

    @Column(name = "left_at")
    private LocalDateTime leftAt;

    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    @CreationTimestamp
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    @UpdateTimestamp
    private LocalDateTime updatedAt;

}
