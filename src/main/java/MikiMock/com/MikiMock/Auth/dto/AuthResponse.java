package MikiMock.com.MikiMock.Auth.dto;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AuthResponse {

    private String accessToken;

    private String tokenType;

    private String refreshToken;

    private String email;

    private String role;

    private Long id;

    private UUID publicId;

    private Integer freeInterviewUsed;

}