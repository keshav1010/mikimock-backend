package MikiMock.com.MikiMock.Interview.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "problem_bank")
public class ProblemBank {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    private String name;

    private Double points;

    private String topic;

    private String level;
}
