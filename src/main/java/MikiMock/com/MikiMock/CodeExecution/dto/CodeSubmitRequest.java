package MikiMock.com.MikiMock.CodeExecution.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CodeSubmitRequest {

    private String roomCode;

    private Long questionId;

    private String language;

    private String code;
}