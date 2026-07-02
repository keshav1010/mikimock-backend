package MikiMock.com.MikiMock.Interview.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "question_test_cases")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionTestCase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_id", nullable = false)
    private InterviewQuestion question;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String inputData;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String expectedOutput;

    @Column(nullable = false)
    private Boolean hidden;

    private Boolean sample;

    private Boolean active;

    @Column(name = "test_order")
    private Integer testOrder;
}