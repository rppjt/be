package runrush.be.user.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import runrush.be.user.domain.User;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByKakaoId(String kakaoId);

    Optional<User> findByEmail(String email);
}