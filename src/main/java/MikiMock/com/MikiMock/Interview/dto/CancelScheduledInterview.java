package MikiMock.com.MikiMock.Interview.dto;

import MikiMock.com.MikiMock.Interview.entity.ScheduleStatus;
import lombok.*;

import java.util.UUID;


@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CancelScheduledInterview {
    private UUID id;

    private ScheduleStatus status;
}
