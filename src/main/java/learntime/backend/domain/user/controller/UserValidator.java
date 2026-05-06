package learntime.backend.domain.user.controller;

import learntime.backend.domain.user.dto.request.SignUpRequestDTO;
import learntime.backend.domain.user.repository.UserRepository;
import learntime.backend.global.error.code.ValidatorErrorCode;
import learntime.backend.global.error.exception.ValidatorException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
public class UserValidator {

    private final UserRepository userRepository;

    // 1. 닉네임 정규식: 한글, 영문, 숫자만 허용 (특수문자 불가)
    private static final String NICKNAME_REGEX = "^[a-zA-Z0-9가-힣]+$";

    // 2. 이메일 정규식: 표준 이메일 형식
    private static final String EMAIL_REGEX = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";

    // 3. 비밀번호 정규식 (SignUpRequestDTO 기준): 8~30자, 영문/숫자/특수문자 포함, 3회 연속 문자 금지
    private static final String PASSWORD_REGEX = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[\\W_])(?!.*(.)\\1\\1).{8,30}$";

    /**
     * 회원가입 통합 유효성 검사 (DTO 기반)
     */
    public void validateSignUp(SignUpRequestDTO request) {
        validateEmail(request.email());
        validateName(request.userName()); // DTO의 userName -> Entity의 name 매핑 확인
        validatePassword(request.password());
    }

    /**
     * 닉네임(이름) 검증: 형식 + 중복 체크
     */
    public void validateName(String name) {
        if (name == null || !Pattern.matches(NICKNAME_REGEX, name)) {
            throw new ValidatorException(ValidatorErrorCode.INVALID_NICKNAME);
        }
        if (userRepository.existsByName(name)) { // Repository의 existsByName 활용
            throw new ValidatorException(ValidatorErrorCode.DUPLICATE_NICKNAME);
        }
    }

    /**
     * 이메일 검증: 형식 + 중복 체크
     */
    public void validateEmail(String email) {
        if (email == null || !Pattern.matches(EMAIL_REGEX, email)) {
            throw new ValidatorException(ValidatorErrorCode.INVALID_EMAIL);
        }
        if (userRepository.existsByEmail(email)) {
            throw new ValidatorException(ValidatorErrorCode.DUPLICATE_EMAIL);
        }
    }

    /**
     * 비밀번호 검증: 정규식 기반 형식 체크
     */
    public void validatePassword(String password) {
        if (password == null || !Pattern.matches(PASSWORD_REGEX, password)) {
            throw new ValidatorException(ValidatorErrorCode.INVALID_PASSWORD);
        }
    }
}
