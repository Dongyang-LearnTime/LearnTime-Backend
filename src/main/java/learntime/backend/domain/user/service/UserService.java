package learntime.backend.domain.user.service;

import learntime.backend.domain.user.model.User;
import learntime.backend.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    // 회원가입
    @Transactional
    public void createUser(String userName, String email, String password) {
        String encodedPassword = passwordEncoder.encode(password); // 비밀번호 암호화

        User user = User.builder()
                .name(userName)
                .email(email)
                .password(encodedPassword)
                .role(User.Role.ROLE_ADMIN) // 관리자는 ROLE_ADMIN, 유저는 ROLE_USER
                .build();

        userRepository.save(user);
    }
}
