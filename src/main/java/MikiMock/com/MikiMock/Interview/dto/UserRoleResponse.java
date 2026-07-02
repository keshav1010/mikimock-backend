package MikiMock.com.MikiMock.Interview.dto;

import MikiMock.com.MikiMock.Interview.entity.ParticipantRole;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserRoleResponse {

    private Long userId;

    private ParticipantRole role;
}