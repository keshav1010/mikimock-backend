package MikiMock.com.MikiMock.Interview.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RoomLanguageChangedEvent {

    private String eventType;

    private String roomCode;

    private String language;
}