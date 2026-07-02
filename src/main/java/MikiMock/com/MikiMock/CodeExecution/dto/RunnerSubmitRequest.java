package MikiMock.com.MikiMock.CodeExecution.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RunnerSubmitRequest {

    private String language;

    private String code;

    private ExecutionConfig executionConfig;

    private List<RunnerTestCase> testCases;
}