package MikiMock.com.MikiMock.Interview.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "interview_questions_theory")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InterviewQuestionTheory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String question;

    @Column(nullable = false)
    private String topic; // DSA

    @Column(nullable = false)
    private String level; // EASY, MEDIUM, HARD

    @Column(nullable = false)
    private Boolean active;

    @Column(nullable = false)
    private String category;

    private LocalDateTime createdAt;


    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();

        if (active == null) {
            active = true;
        }


    }
}
