package MikiMock.com.MikiMock.Interview.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FutureScheduledInterviews {

    private UUID id;

    private String problem;

    private String topic;

    private String level;

    private LocalDateTime scheduledTime;

    private LocalDateTime endTime;

}
