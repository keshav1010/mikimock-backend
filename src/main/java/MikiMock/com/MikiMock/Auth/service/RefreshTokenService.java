package MikiMock.com.MikiMock.Auth.service;


import MikiMock.com.MikiMock.Common.Exception.BusinessException;
import MikiMock.com.MikiMock.Auth.entity.RefreshToken;
import MikiMock.com.MikiMock.Auth.repository.RefreshTokenRepository;
import MikiMock.com.MikiMock.User.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;

    @Transactional
    public RefreshToken createRefreshToken(
            User user
    ) {

        RefreshToken refreshToken = refreshTokenRepository
                        .findByUser(user)
                        .orElse(null);

        if(refreshToken == null){
            refreshToken = new RefreshToken();
            refreshToken.setUser(user);
        }

        log.info("creating new RefreshToken");
        refreshToken.setToken(UUID.randomUUID().toString());
        refreshToken.setRevoked(false);
        refreshToken.setCreatedAt(LocalDateTime.now());
        refreshToken.setExpiryAt(LocalDateTime.now().plusDays(7));

        return refreshTokenRepository.save(refreshToken);
    }

    public RefreshToken validateRefreshToken(String token) {

        RefreshToken refreshToken = refreshTokenRepository.findByToken(token).orElseThrow(() -> new BusinessException(
                                        "Invalid refresh token"
                                ));

        if (refreshToken.getRevoked()) {throw new BusinessException(
                    "Refresh token revoked");
        }

        if (refreshToken.getExpiryAt().isBefore(LocalDateTime.now())) {throw new BusinessException(
                    "Refresh token expired"
            );}

        return refreshToken;
    }

    public void revokeToken(User user) {

        refreshTokenRepository.findByUser(user)

                .ifPresent(token -> {

                    token.setRevoked(true);

                    refreshTokenRepository.save(token);
                });
    }
}