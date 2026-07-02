package MikiMock.com.MikiMock.CodeExecution.dto;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CodeRunResponse {

    private Boolean success;

    private String output;

    private String error;

    private String status;

    private Long executionTimeMs;
}