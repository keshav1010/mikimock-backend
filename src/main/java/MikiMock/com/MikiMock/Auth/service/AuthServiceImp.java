package MikiMock.com.MikiMock.Auth.service;


import MikiMock.com.MikiMock.Auth.dto.AuthResponse;
import MikiMock.com.MikiMock.Auth.dto.LoginRequest;
import MikiMock.com.MikiMock.Auth.dto.RefreshTokenResponse;
import MikiMock.com.MikiMock.Auth.dto.RegisterRequest;
import MikiMock.com.MikiMock.Common.Exception.BusinessException;

import MikiMock.com.MikiMock.Payment.repository.UserCredentialsRepository;
import MikiMock.com.MikiMock.Auth.entity.RefreshToken;
import MikiMock.com.MikiMock.Security.JWT.JwtService;
import MikiMock.com.MikiMock.User.entity.User;
import MikiMock.com.MikiMock.User.entity.UserCredentials;
import MikiMock.com.MikiMock.User.repository.UserRepository;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuthServiceImp implements AuthService{

    private final RefreshTokenService refreshTokenService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
    private final UserCredentialsRepository userCredentialsRepository;

    @Override
    public AuthResponse register(RegisterRequest registerRequest) {
        if(userRepository.existsByEmail(
                registerRequest.getEmail()
        )) {

            throw new BusinessException(
                    "Email already exists"
            );
        }

        User user = User.builder()

                .email(registerRequest.getEmail())

                .password(
                        passwordEncoder.encode(
                                registerRequest.getPassword()
                        )
                )
                .build();

        userRepository.save(user);

        UserCredentials userCredentials = UserCredentials.builder()
                        .user(user)
                .password(user.getPassword())
                .email(user.getEmail())
                .failedLoginAttempts(0)
                .build();

        userCredentialsRepository.save(userCredentials);

        String accessToken =
                jwtService.generateToken(user);

        String refreshToken =
                refreshTokenService
                        .createRefreshToken(user)
                        .getToken();

        return AuthResponse.builder()

                .accessToken(accessToken)

                .refreshToken(refreshToken)

                .tokenType("Bearer")

                .email(user.getEmail())

                .role(user.getRole().name())

                .build();

    }


    @Override
    public AuthResponse login(LoginRequest loginRequest) {
            try {
                authenticationManager.authenticate(
                        new UsernamePasswordAuthenticationToken(loginRequest.getEmail(),loginRequest.getPassword())
                );
            } catch (Exception e) {
                throw new BusinessException("Invalid Credentials");
            }

            User user = userRepository.findByEmail(loginRequest.getEmail()).orElseThrow(() ->
                    new BusinessException("Invalid Credentials"));


            UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());

        String accessToken =
                jwtService.generateToken(user);

        String refreshToken =
                refreshTokenService
                        .createRefreshToken(user)
                        .getToken();

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .email(user.getEmail())
                .role(user.getRole().name())
                .id(user.getId())
                .freeInterviewUsed(user.getFreeInterviewUsed())
                .publicId(user.getPublicId())
                .build();
    }

    @Override
    public RefreshTokenResponse refreshToken(String refreshTokenValue) {
        RefreshToken refreshToken = refreshTokenService.validateRefreshToken(refreshTokenValue);

        User user = refreshToken.getUser();

        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());

        String accessToken = jwtService.generateToken(userDetails);

        return RefreshTokenResponse.builder()
                .accessToken(accessToken)
                .expiresIn(900L)
                .build();
    }

    @Override
    public String logout(HttpServletRequest request, HttpServletResponse response) {

        String authHeader = request.getHeader("Authorization");

        String token = authHeader.substring(7);

        String email = jwtService.extractUsername(token);

        log.info("user = {}",email);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    throw new BusinessException("User not found");
                });

        // 1. Revoke refresh token from DB
        refreshTokenService.revokeToken(user);

        // 2. Clear cookie
        Cookie cookie = new Cookie("refresh_token", null);
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath("/");
        cookie.setMaxAge(0);

        response.addCookie(cookie);

        // 3. Clear security context
        SecurityContextHolder.clearContext();
        return "Logout";
    }
}

