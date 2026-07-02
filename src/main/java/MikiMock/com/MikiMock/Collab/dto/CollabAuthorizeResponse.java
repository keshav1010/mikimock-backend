package MikiMock.com.MikiMock.Collab.dto;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CollabAuthorizeResponse {

    private String roomCode;

    private Long userId;

    private Boolean allowed;
}