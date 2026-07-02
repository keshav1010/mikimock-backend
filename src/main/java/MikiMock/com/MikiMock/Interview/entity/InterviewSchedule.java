package MikiMock.com.MikiMock.Interview.entity;

import MikiMock.com.MikiMock.User.entity.User;
import jakarta.persistence.*;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "interview_schedule",
        indexes = {
                @Index(name = "idx_interview_schedule_user", columnList = "user_id"),
                @Index(name = "idx_interview_schedule_status", columnList = "status"),
                @Index(name = "idx_interview_schedule_time", columnList = "scheduled_time")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InterviewSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(
            name = "id",
            nullable = false,
            updatable = false
    )
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "user_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_interview_schedule_user")
    )
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "partner_user_id"
    )
    private User partner;

    @NotBlank
    @Size(max = 100)
    @Column(
            name = "topic",
            nullable = false,
            length = 100
    )
    private String topic;

    @NotBlank
    @Size(max = 30)
    @Column(
            name = "level",
            nullable = false,
            length = 30
    )
    private String level;

    @NotNull
    @Future
    @Column(
            name = "scheduled_time",
            nullable = false
    )
    private LocalDateTime scheduledTime;

    @NotNull
    @Future
    @Column(
            name = "end_time",
            nullable = false
    )
    private LocalDateTime endTime;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(
            name = "status",
            nullable = false,
            length = 30
    )
    private ScheduleStatus status;

    @ManyToOne
    @JoinColumn(name = "problem_id")
    private ProblemBank problem;

    @ManyToOne
    @JoinColumn(name = "assigned_problem_id")
    private InterviewQuestion assignedProblem;

    @ManyToOne
    @JoinColumn(name = "solved_problem_id")
    private InterviewQuestion solvedProblem;


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
        LocalDateTime now = LocalDateTime.now();

        this.createdAt = now;
        this.updatedAt = now;

        if (this.status == null) {
            this.status = ScheduleStatus.SCHEDULED;
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}