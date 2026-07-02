package MikiMock.com.MikiMock.Auth.service;

import MikiMock.com.MikiMock.Auth.dto.AuthResponse;
import MikiMock.com.MikiMock.Auth.dto.LoginRequest;
import MikiMock.com.MikiMock.Auth.dto.RefreshTokenResponse;
import MikiMock.com.MikiMock.Auth.dto.RegisterRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.stereotype.Service;


@Service
public interface AuthService {
    AuthResponse register(@Valid RegisterRequest registerRequest);

    AuthResponse login(@Valid LoginRequest loginRequest);

    RefreshTokenResponse refreshToken(String refreshToken);

    String logout(HttpServletRequest request, HttpServletResponse response);
}
