package MikiMock.com.MikiMock.CodeExecution.dto;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RunnerRunRequest {

    private String language;

    private String code;

    private ExecutionConfig executionConfig;

    private String input;
}
