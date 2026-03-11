package learntime.backend.domain.user.service;

import learntime.backend.domain.user.repository.UserRepository;
import learntime.backend.domain.user.model.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
@ExtendWith(MockitoExtension.class)
class CreateUserTest {

    @InjectMocks // 가짜 객체들(@Mock)을 주입받을 진짜 테스트 대상 객체
    private UserService userService;

    @Mock // DB에 실제로 접근하지 않도록 가짜(Mock) Repository 생성
    private UserRepository userRepository;

    @Mock // 실제 암호화 로직을 수행하지 않도록 가짜(Mock) Encoder 생성
    private PasswordEncoder passwordEncoder;

    @Test
    @DisplayName("회원가입 로직이 정상적으로 동작하며 비밀번호가 암호화되어 저장된다.")
    void createUser_Success() {
        // given (준비 단계)
        String rawPassword = "mySecretPassword123!";
        String encodedPassword = "encodedPassword123!";

        // passwordEncoder.encode()가 호출되면 가짜 암호화된 문자열을 반환하도록 설정 (Stubbing)
        given(passwordEncoder.encode(rawPassword)).willReturn(encodedPassword);

        // userRepository.save()가 호출되면 그냥 아무 동작 없이 통과하도록 설정
        given(userRepository.save(any(User.class))).willAnswer(invocation -> invocation.getArgument(0));

        // when (실행 단계)
        userService.createUser("홍길동", "test@email.com", rawPassword);

        // then (검증 단계)
        // 1. 비밀번호 암호화 메서드가 1번 호출되었는가?
        verify(passwordEncoder).encode(rawPassword);
        // 2. UserRepository의 save 메서드가 User 엔티티를 담아서 1번 호출되었는가?
        verify(userRepository).save(any(User.class));
    }
}