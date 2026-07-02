package MikiMock.com.MikiMock.CodeExecution.dto;


import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CodeTestResult {

    private Integer testCaseNumber;

    private String input;

    private String expectedOutput;

    private String actualOutput;

    private Boolean passed;

    private String error;
}