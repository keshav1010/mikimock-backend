package MikiMock.com.MikiMock.Analytics.dto;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TodayDto {

    private Long usersRegisteredToday;

    private Long premiumPurchasedToday;

    private Long interviewsCompletedToday;

    private Long roomsCreatedToday;

    private Long noShowToday;

    private BigDecimal todayRevenue;

    private Long pendingContactMessages;

}
