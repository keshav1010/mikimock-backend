package MikiMock.com.MikiMock.Security.OAuth;

import MikiMock.com.MikiMock.Auth.dto.GeneratedRefreshToken;
import MikiMock.com.MikiMock.Auth.entity.RefreshToken;
import MikiMock.com.MikiMock.Auth.service.RefreshTokenService;
import MikiMock.com.MikiMock.Security.JWT.JwtService;
import MikiMock.com.MikiMock.Security.Service.CustomUserDetailsService;
import MikiMock.com.MikiMock.User.entity.AuthProvider;
import MikiMock.com.MikiMock.User.entity.User;
import MikiMock.com.MikiMock.User.entity.UserRoles;
import MikiMock.com.MikiMock.User.repository.UserRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Map;

import static MikiMock.com.MikiMock.User.entity.AuthProvider.GOOGLE;

@Component
@Slf4j
@RequiredArgsConstructor
public class OAuthSuccessHandler
        implements AuthenticationSuccessHandler {

    private final UserRepository userRepository;

    private final JwtService jwtService;

    private final RefreshTokenService refreshTokenService;

    private final ObjectMapper objectMapper;

    private final CustomUserDetailsService customUserDetailsService;

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException, ServletException {

        CustomOAuth2User principal =
                (CustomOAuth2User) authentication.getPrincipal();

        OAuthUser oauthUser =
                principal.getOauthUser();

        User user =
                userRepository.findByEmail(oauthUser.getEmail())
                        .orElseGet(() -> createUser(oauthUser));

        UserDetails userDetails =
                customUserDetailsService
                        .loadUserByUsername(user.getEmail());

        String accessToken =
                jwtService.generateToken(userDetails);

        GeneratedRefreshToken generatedRefreshToken =
                refreshTokenService.createRefreshToken(user);

        Cookie cookie =
                new Cookie(
                        "refresh_token",
                        generatedRefreshToken.getRawToken()
                );

        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath("/api/v1/auth");
        cookie.setMaxAge(60 * 60 * 24 * 30);

        response.addCookie(cookie);

        response.setContentType("application/json");

        String redirectUrl =
                "http://localhost:5173/oauth/success?token="
                        + URLEncoder.encode(accessToken, StandardCharsets.UTF_8);

        response.sendRedirect(redirectUrl);
    }

    private User createUser(
            OAuthUser oauthUser
    ) {

        User user =
                User.builder()
                        .email(oauthUser.getEmail())
                        .provider(GOOGLE)
                        .providerId(oauthUser.getProviderId())
                        .profileImage(oauthUser.getPicture())
                        .role(UserRoles.USER)
                        .build();

        return userRepository.save(user);
    }
}