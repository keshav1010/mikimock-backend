package MikiMock.com.MikiMock.Auth.controller;

import MikiMock.com.MikiMock.Auth.dto.*;
import MikiMock.com.MikiMock.Auth.service.AuthService;
import MikiMock.com.MikiMock.Common.Exception.BusinessException;
import MikiMock.com.MikiMock.Common.Response.ApiResponse;
import MikiMock.com.MikiMock.Common.Response.ResponseUtil;
import MikiMock.com.MikiMock.User.entity.User;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @RateLimiter(name = "authRateLimiter")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest registerRequest) {

        String correlationId = MDC.get("X-Correlation-Id");

        log.info("Registration request received | correlationId={} | email={}", correlationId, registerRequest.getEmail());

        AuthResponse response = authService.register(registerRequest);

        log.info(
                "Registration successful | correlationId={} | email={}",
                correlationId,
                registerRequest.getEmail()
        );

        return ResponseUtil.created(
                "User registered successfully",
                response
        );
    }

    @PostMapping("/login")
    @RateLimiter(name = "authRateLimiter")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest loginRequest , HttpServletResponse cookieResponse) {

        String correlationId = MDC.get("X-Correlation-Id");

        log.info("Login request received | correlationId={} | email={}", correlationId, loginRequest.getEmail());

        AuthResponse response = authService.login(loginRequest);

        Cookie cookie = new Cookie("refresh_token", response.getRefreshToken());

        cookie.setHttpOnly(true);

        cookie.setSecure(true);                           //true when production

        cookie.setPath("/");

        cookie.setMaxAge(7 * 24 * 60 * 60);

        cookieResponse.addCookie(cookie);

        response.setRefreshToken(null);

        log.info("Login successful | correlationId={} | email={}", correlationId, loginRequest.getEmail());


        return ResponseUtil.success("Login Successful", response);
    }

    @PostMapping("/refresh")
    @RateLimiter(name = "authRateLimiter")
    public ResponseEntity<ApiResponse<RefreshTokenResponse>> refreshToken(HttpServletRequest request) {

        if (request.getCookies() == null) {throw new BusinessException("Refresh token missing");}

        String refreshToken = Arrays.stream(request.getCookies())
                .filter(cookie -> cookie.getName().equals("refresh_token")
                )

                .findFirst()
                .map(Cookie::getValue)
                .orElseThrow(() ->
                        new BusinessException(
                                "Refresh token missing"
                        )
                );

        RefreshTokenResponse response = authService.refreshToken(refreshToken);

        return ResponseUtil.success("Token generated",response);
    }


    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request, HttpServletResponse response) {

        String correlationId = MDC.get("X-Correlation-Id");

        log.info("Logout request received | correlationId={}", correlationId);
        String reponse = authService.logout(request, response);
        return ResponseUtil.success("Logout successful",reponse);
    }


    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<String>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {

        authService.forgotPassword(request.getEmail());

        return ResponseUtil.success(
                "If account exists, a password has been sent."
        ,"If account exists, a password has been sent.");
    }

    @PostMapping("/change-password")
    public ResponseEntity<ApiResponse<String>> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        log.info("Change Password Request");

        authService.changePassword(request);

        return ResponseUtil.success(
                "Password Changes."
                ,"Password Changes.");
    }

}
