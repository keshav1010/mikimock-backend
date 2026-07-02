package MikiMock.com.MikiMock.User.dto;


import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InterviewListResponse {

    private Long interviewId;

    private String problem;

    private String topic;

    private String level;

    private LocalDateTime scheduledTime;

    private String pertnerEmail;

    private String status;
}
