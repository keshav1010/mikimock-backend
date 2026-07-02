package MikiMock.com.MikiMock.User.repository;

import MikiMock.com.MikiMock.User.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository
        extends JpaRepository<User, Long> {

    Optional<User> findByEmail(
            String email
    );

    Optional<User> findByPublicId(
            UUID publicId
    );

    boolean existsByEmail(
            String email
    );
}