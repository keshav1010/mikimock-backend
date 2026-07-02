package MikiMock.com.MikiMock.Interview.dto;

import MikiMock.com.MikiMock.Interview.entity.RoomMode;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@Builder
public class JoinInterviewResponse {

//    private UUID roomId;
//
    private String roomCode;

    private RoomMode roomMode;
//
//    private String agoraToken;
//
//    private String agoraChannel;
//
//    private Integer agoraUid;

    private String status;
}