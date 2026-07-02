package MikiMock.com.MikiMock.User.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class UserAuthMeResponse {
    private Long id;

    private UUID publicId;

    private String email;

    private String fullName;

    private String role;

    private String subscriptionType;

    private Integer freeInterviewUsed;

    private Boolean isEmailVerified;

    private LocalDateTime createdAt;
}