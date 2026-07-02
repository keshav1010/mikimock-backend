package MikiMock.com.MikiMock.CodeExecution.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CodeRunRequest {

    private String roomCode;

    private Long questionId;

    private String language;

    private String code;

    private String input;
}