package MikiMock.com.MikiMock.User.dto;


import lombok.*;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InterviewDateResponse {

    private LocalDateTime interviewDates;
}
