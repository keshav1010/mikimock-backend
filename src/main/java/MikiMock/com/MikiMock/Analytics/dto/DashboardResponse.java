package MikiMock.com.MikiMock.Analytics.dto;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardResponse {

    private OverviewDto overview;

    private TodayDto today;

    private SystemHealthDto system;

}
