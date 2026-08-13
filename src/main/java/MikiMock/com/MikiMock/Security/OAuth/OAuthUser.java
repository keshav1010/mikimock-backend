package MikiMock.com.MikiMock.Security.OAuth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class OAuthUser {

    private String email;

    private String name;

    private String providerId;

    private String picture;
}