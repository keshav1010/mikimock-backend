package MikiMock.com.MikiMock.Interview.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class RoomJoinResponse {

    private String appId;

    private String channelName;

    private String token;

    private Integer uid;
}
