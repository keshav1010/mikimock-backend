package MikiMock.com.MikiMock.Interview.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoleChangeResponse {
    private String eventType;

    private List<UserRoleResponse> roles;

    private AssignedProblemResponse problem;
}