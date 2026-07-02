package MikiMock.com.MikiMock.Interview.services;
import io.agora.media.RtcTokenBuilder2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class AgoraTokenService {

    @Value("${agora.app-id}")
    private String appId;

    @Value("${agora.app-certificate}")
    private String appCertificate;

    @Value("${agora.token-expiry}")
    private Integer tokenExpiry;

    public String generateToken(
            String channelName,
            Integer uid
    ) {

        int timestamp =
                (int) (
                        System.currentTimeMillis()
                                / 1000
                );

        int privilegeExpiredTs =
                timestamp + tokenExpiry;

        RtcTokenBuilder2 tokenBuilder = new RtcTokenBuilder2();
        return tokenBuilder.buildTokenWithUid(
                appId,
                appCertificate,
                channelName,
                uid,
                RtcTokenBuilder2.Role.ROLE_PUBLISHER,
                privilegeExpiredTs,
                privilegeExpiredTs
        );
    }
}
