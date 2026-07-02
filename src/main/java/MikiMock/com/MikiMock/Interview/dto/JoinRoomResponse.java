package MikiMock.com.MikiMock.Interview.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JoinRoomResponse {

    private String roomCode;

    private String roomStatus;

    private String roomMode;

    private String lanuage;

    private String appId;

    private String channelName;

    private String token;

    private Integer uid;

    private String myRole;

    private String partnerRole;

    private String topic;

    private String level;

    private Long remainingSeconds;

    private AssignedProblemResponse problem;

    private List<AssignedProblemResponse> problems;
}