package MikiMock.com.MikiMock.Interview.dto;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProblemChangedEvent {

    private String eventType;

    private AssignedProblemResponse problem;
}
