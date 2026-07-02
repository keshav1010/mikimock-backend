package MikiMock.com.MikiMock.Interview.dto;


import MikiMock.com.MikiMock.Interview.entity.ParticipantRole;
import MikiMock.com.MikiMock.Interview.entity.RoomMode;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoomContextResponse {

    private String roomCode;

    private RoomMode roomMode;

    private ParticipantRole myRole;

    private String language;

    private ParticipantRole partnerRole;

    private String topic;

    private String level;
}