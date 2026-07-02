package MikiMock.com.MikiMock.Payment.repository;

import MikiMock.com.MikiMock.User.entity.UserCredentials;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserCredentialsRepository extends JpaRepository<UserCredentials,Long> {
}
