package MikiMock.com.MikiMock.Auth.service;


import MikiMock.com.MikiMock.Auth.dto.*;
import MikiMock.com.MikiMock.Common.Exception.BusinessException;

import MikiMock.com.MikiMock.Notification.dto.NotificationEvent;
import MikiMock.com.MikiMock.Notification.producer.NotificationProducer;
import MikiMock.com.MikiMock.Payment.repository.UserCredentialsRepository;
import MikiMock.com.MikiMock.Auth.entity.RefreshToken;
import MikiMock.com.MikiMock.Security.JWT.JwtService;
import MikiMock.com.MikiMock.User.entity.AuthProvider;
import MikiMock.com.MikiMock.User.entity.User;
import MikiMock.com.MikiMock.User.entity.UserCredentials;
import MikiMock.com.MikiMock.User.repository.UserRepository;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Optional;

import static MikiMock.com.MikiMock.User.entity.AuthProvider.GOOGLE;

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
    private final NotificationProducer notificationProducer;

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest registerRequest) {
        if(userRepository.existsByEmail(registerRequest.getEmail())) {
            throw new BusinessException("Email already exists");
        }

        User user = User.builder()
                .email(registerRequest.getEmail())
                .password(passwordEncoder.encode(registerRequest.getPassword()))
                .build();

        userRepository.save(user);

        UserCredentials userCredentials = UserCredentials.builder()
                        .user(user)
                .password(user.getPassword())
                .email(user.getEmail())
                .failedLoginAttempts(0)
                .build();

        userCredentialsRepository.save(userCredentials);

        String accessToken = jwtService.generateToken(user);

        String refreshToken = refreshTokenService.createRefreshToken(user).getRawToken();

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
                jwtService.generateToken(userDetails);

        String refreshToken =
                refreshTokenService
                        .createRefreshToken(user)
                        .getRawToken();

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
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new BusinessException("Invalid token");
        }

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

    @Override
    public void forgotPassword(String email) {
        log.info("Forget password called with email | {}",email);
        Optional<User> optionalUser = userRepository.findByEmail(email);

        if (optionalUser.isEmpty()) {
            return;
        }

        User user = optionalUser.get();
        log.info("User is there | {}",user.getEmail());

        if (user.getProvider() == AuthProvider.GOOGLE) {
            log.info("Google user cannot reset password");
            return;
        }
        log.info("User found for forget password | {}",user.getEmail());



//        UserCredentials credentials =userCredentialsRepository.findByEmail(email).orElseThrow();

        String temporaryPassword =
                generateTemporaryPassword();

        user.setPassword(
                passwordEncoder.encode(
                        temporaryPassword
                )
        );
        userRepository.save(user);

//        credentials.setMustChangePassword(
//                true
//        );

//        userCredentialsRepository.save(
//                credentials
//        );

        NotificationEvent event =
                NotificationEvent.builder()

                        .toEmail(email)

                        .subject("Temporary Password")

                        .body(
                                """
                                Your temporary password is:
        
                                %s
        
                                Please login and change it immediately.
                                """.formatted(temporaryPassword)
                        )

                        .build();

        notificationProducer.publish(event);

    }

    @Override
    public void changePassword(ChangePasswordRequest request) {

        log.info("In change password method");
        User user = getAuthenticatedUser();

        if(user.getProvider().equals(GOOGLE)) throw new BusinessException("Can't change password, As you logged in using Oauth");

        // ==========================
        // Validate Current Password
        // ==========================

        if (!passwordEncoder.matches(
                request.getCurrentPassword(),
                user.getPassword()
        )) {

            throw new BusinessException(
                    "Current password is incorrect."
            );
        }
        log.info("After password matched");

        // ==========================
        // Validate New Password Match
        // ==========================

        if (!request.getNewPassword().equals(
                request.getConfirmPassword()
        )) {

            throw new BusinessException(
                    "New password and confirm password do not match."
            );
        }
        log.info("After new passwords matches");

        // ==========================
        // Password Policy
        // ==========================

        String password = request.getNewPassword();

        if (!password.matches(
                "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[@$!%*#?&^()_+=\\-{}\\[\\]:;\"'<>,./]).{6,}$"
        )) {

            throw new BusinessException(
                    "Password must be at least 6 characters long and contain at least one alphabet, one number, and one special character."
            );
        }
        log.info("After password policy");

        // ==========================
        // Prevent Same Password
        // ==========================

        if (passwordEncoder.matches(
                password,
                user.getPassword()
        )) {

            throw new BusinessException(
                    "New password cannot be the same as the current password."
            );
        }
        log.info("After new passwords matches with old password");

        // ==========================
        // Update Password
        // ==========================

        user.setPassword(
                passwordEncoder.encode(password)
        );

        userRepository.save(user);
    }

    private String generateTemporaryPassword() {

        String chars =
                "ABCDEFGHIJKLMNOPQRSTUVWXYZ" +
                        "abcdefghijklmnopqrstuvwxyz" +
                        "0123456789" +
                        "@#$%&*";

        SecureRandom random =
                new SecureRandom();

        StringBuilder sb =
                new StringBuilder();

        for(int i=0;i<12;i++){

            sb.append(
                    chars.charAt(
                            random.nextInt(
                                    chars.length()
                            )
                    )
            );
        }

        return sb.toString();
    }

    private User getAuthenticatedUser(){
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if(authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken){
            log.warn("Unauthorized access attempt | correlationId={}", MDC.get("X-Correlation-Id"));
            throw new BusinessException("User not authenticated");
        }
        String email = authentication.getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    throw new BusinessException("User not found");
                });
        return user;
    }
}

