package MikiMock.com.MikiMock.User.repository;

import MikiMock.com.MikiMock.User.entity.User;
import MikiMock.com.MikiMock.User.entity.UserSkill;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserSkillRepository extends JpaRepository<UserSkill,Long> {

    Optional<UserSkill> findByUser(User user);
}
