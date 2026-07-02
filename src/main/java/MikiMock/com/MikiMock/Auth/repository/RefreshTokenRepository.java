package MikiMock.com.MikiMock.Auth.repository;


import MikiMock.com.MikiMock.Auth.entity.RefreshToken;
import MikiMock.com.MikiMock.User.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository
        extends JpaRepository<RefreshToken, UUID> {

    Optional<RefreshToken>
    findByToken(String token);

    Optional<RefreshToken>
    findByUser(User user);

    void deleteByUser(User user);
}
