package MikiMock.com.MikiMock.CodeExecution.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ExecutionConfig {

    private String functionName;

    private List<ExecutionParam> params;

    private String returnType;

    private CompareMode compareMode =
            CompareMode.EXACT;

    private CheckerType checkerType =
            CheckerType.DEFAULT;

    private String outputParam;

    private Integer timeLimitMs = 5000;

    private Integer memoryLimitMb = 256;
}
