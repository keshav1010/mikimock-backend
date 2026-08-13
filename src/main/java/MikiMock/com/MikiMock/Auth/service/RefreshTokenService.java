package MikiMock.com.MikiMock.Auth.service;


import MikiMock.com.MikiMock.Auth.dto.GeneratedRefreshToken;
import MikiMock.com.MikiMock.Common.Exception.BusinessException;
import MikiMock.com.MikiMock.Auth.entity.RefreshToken;
import MikiMock.com.MikiMock.Auth.repository.RefreshTokenRepository;
import MikiMock.com.MikiMock.User.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.UUID;

import static java.util.Objects.hash;

@Slf4j
@Transactional
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository repository;

    @Transactional
    public GeneratedRefreshToken createRefreshToken(
            User user
    ) {

        RefreshToken refreshToken =
                repository.findByUser(user)
                        .orElse(new RefreshToken());

        refreshToken.setUser(user);

        String rawToken =
                generateRefreshToken();

        refreshToken.setTokenHash(
                hash(rawToken)
        );

        refreshToken.setRevoked(false);

        refreshToken.setExpiryAt(
                LocalDateTime.now().plusDays(7)
        );

        repository.save(refreshToken);

        return new GeneratedRefreshToken(
                refreshToken,
                rawToken
        );
    }

    public RefreshToken validateRefreshToken(
            String rawToken
    ) {

        String hashed =
                hash(rawToken);

        RefreshToken refreshToken =
                repository.findByTokenHash(hashed)
                        .orElseThrow(() ->
                                new BusinessException("Invalid refresh token")
                        );

        if (refreshToken.getRevoked()) {
            throw new BusinessException("Refresh token revoked");
        }

        if (refreshToken.getExpiryAt().isBefore(LocalDateTime.now())) {
            throw new BusinessException("Refresh token expired");
        }

        return refreshToken;
    }

    public void revokeToken(User user) {

        repository.findByUser(user)
                .ifPresent(token -> {

                    token.setRevoked(true);

                    repository.save(token);
                });
    }

    private String hash(String token) {

        return DigestUtils.sha256Hex(token);
    }

    private String generateRefreshToken() {

        byte[] bytes =
                new byte[32];

        new SecureRandom().nextBytes(bytes);

        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(bytes);
    }
}