package MikiMock.com.MikiMock.InterviewIDE.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CodeUpdateRequest {

    private String roomCode;

    private String code;

    private String language;
}