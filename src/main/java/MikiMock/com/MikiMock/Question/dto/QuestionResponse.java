package MikiMock.com.MikiMock.Question.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class QuestionResponse {

    private Long id;

    private String title;

    private String category;

    private String level;

    private String company;

    private String solve;
}