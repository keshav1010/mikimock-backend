package MikiMock.com.MikiMock.Interview.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class ScheduleRequest {

    @NotBlank(message = "Topic is required")
    @Size(max = 100, message = "Topic cannot exceed 100 characters")
    private String topic;

    @NotBlank(message = "Level is required")
    @Size(max = 30, message = "Level cannot exceed 30 characters")
    private String level;

    @NotNull(message = "Time is required")
    @Future(message = "Time must be in the future")
    private LocalDateTime time;
}
