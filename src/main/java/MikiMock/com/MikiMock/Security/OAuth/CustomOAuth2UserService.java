package MikiMock.com.MikiMock.Security.OAuth;

import MikiMock.com.MikiMock.User.entity.User;
import MikiMock.com.MikiMock.User.entity.UserRoles;
import MikiMock.com.MikiMock.User.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Optional;

import static MikiMock.com.MikiMock.User.entity.AuthProvider.GOOGLE;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService
        extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest request) throws OAuth2AuthenticationException {

        OAuth2User googleUser = super.loadUser(request);

        String email =
                googleUser.getAttribute("email");

        String name =
                googleUser.getAttribute("name");

        String providerId =
                googleUser.getAttribute("sub");

        String picture =
                googleUser.getAttribute("picture");



        OAuthUser oauthUser =
                OAuthUser.builder()
                        .email(email)
                        .name(name)
                        .providerId(providerId)
                        .picture(picture)
                        .build();


        User user = User.builder()
                        .email(oauthUser.getEmail())
                        .provider(GOOGLE)
                        .providerId(oauthUser.getProviderId())
                        .profileImage(oauthUser.getPicture())
                        .role(UserRoles.USER)
                        .build();

//        else {                                                      // Link to exisitng user (can login both ways)
//            if (user.getProvider() == "LOCAL") {
//
//                user.setProvider(GOOGLE);
//
//                user.setProviderId(providerId);
//
//                user.setProfileImage(picture);
//
//                repository.save(user);
//            }
//        }

        return new CustomOAuth2User(
                googleUser,
                oauthUser
        );
    }

}
