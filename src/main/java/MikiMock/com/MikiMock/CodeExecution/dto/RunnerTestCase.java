package MikiMock.com.MikiMock.CodeExecution.dto;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RunnerTestCase {

    private Integer testCaseNumber;

    private String input;

    private String expectedOutput;
}