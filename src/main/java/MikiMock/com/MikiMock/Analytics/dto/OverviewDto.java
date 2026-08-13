package MikiMock.com.MikiMock.Analytics.dto;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OverviewDto {

    private Long totalUsers;

    private Long premiumUsers;

    private Long freeUsers;

    private Long totalInterviews;

    private Long activeInterviews;

    private Long waitingUsers;

}
