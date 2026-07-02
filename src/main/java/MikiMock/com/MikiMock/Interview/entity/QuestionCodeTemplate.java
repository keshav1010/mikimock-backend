package MikiMock.com.MikiMock.Interview.entity;

import MikiMock.com.MikiMock.CodeExecution.dto.ExecutionConfig;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.UUID;

@Entity
@Table(
        name = "question_code_templates",
        uniqueConstraints = {
                @UniqueConstraint(
                        columnNames = {"question_id", "language"}
                )
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionCodeTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_id", nullable = false)
    private InterviewQuestion question;

    @Column(nullable = false)
    private String language; // java, python, cpp

    @Column(columnDefinition = "TEXT", nullable = false)
    private String starterCode;

    @Column(name = "function_name")
    private String functionName;

    @Column(name = "input_type")
    private String inputType;

    @Column(name = "return_type")
    private String returnType;

    @Column(name = "execution_config", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private ExecutionConfig executionConfig;
}
