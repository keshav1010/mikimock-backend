package MikiMock.com.MikiMock.CodeExecution.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CodeSubmitResponse {

    private Integer totalTests;

    private Integer passedTests;

    private Boolean allPassed;

    private List<CodeTestResult> results;
}