package runrush.be.user.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import runrush.be.user.domain.User;
import runrush.be.user.repository.UserRepository;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;

    public User findUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
    }
}