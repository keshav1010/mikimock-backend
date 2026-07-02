package MikiMock.com.MikiMock.Interview.entity;


import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "interview_questions")
@Getter
@Setter
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InterviewQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String description;

    @Column(nullable = false)
    private String topic; // DSA

    @Column(nullable = false)
    private String level; // EASY, MEDIUM, HARD

    @Column(columnDefinition = "TEXT")
    private String examples;

    @Column(columnDefinition = "TEXT")
    private String constraints;

    @Column(nullable = false)
    private Boolean active;

    @Column(nullable = false)
    private String category;

    private LocalDateTime createdAt;

    private Long score;

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();

        if (active == null) {
            active = true;
        }
        if (category == null) {
            category = "GENERAL";
        }


    }


}