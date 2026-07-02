package MikiMock.com.MikiMock.Interview.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssignedProblemResponse {

    private Long questionId;

    private String title;

    private String description;

    private String topic;

    private String level;

    private String examples;

    private String constraints;

    private String language;

    private String starterCode;

    private List<String> theoryQuestions;

}