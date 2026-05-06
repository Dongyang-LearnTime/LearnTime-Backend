package learntime.backend.domain.user.service;

import learntime.backend.domain.user.controller.UserValidator;
import learntime.backend.domain.user.dto.request.SignUpRequestDTO;
import learntime.backend.domain.user.dto.response.MyPageResponseDTO;
import learntime.backend.domain.user.model.User;
import learntime.backend.domain.user.repository.RefreshTokenRepository;
import learntime.backend.domain.user.repository.UserRepository;
import learntime.backend.global.config.security.CustomPasswordEncoder;
import learntime.backend.global.error.code.AuthErrorCode;
import learntime.backend.global.error.exception.AuthException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserValidator userValidator;
    private final CustomPasswordEncoder passwordEncoder;
    private final RefreshTokenRepository refreshTokenRepository;

    // 내 정보 조회
    @Transactional(readOnly = true)
    public MyPageResponseDTO getMyInfo(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AuthException(AuthErrorCode.USER_NOT_FOUND));
        return MyPageResponseDTO.from(user);
    }

    // 내 정보 수정
    @Transactional
    public MyPageResponseDTO updateMyInfo(String email, SignUpRequestDTO request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AuthException(AuthErrorCode.USER_NOT_FOUND));

        // 1. 닉네임 변경 시 유효성 검사
        if (!user.getName().equals(request.userName())) {
            userValidator.validateName(request.userName());
            user.updateInfo(request.userName());
        }

        // 2. 비밀번호 업데이트 로직 (값이 있을 때만 실행)
        if (request.password() != null && !request.password().isBlank()) {
            // 2-1. 정규식 유효성 검증 (8~30자, 영문/숫자/특수문자 등) -> 회원가입에 사용된 SignUpRequestDTO와 같은 유효성 기준 부여
            userValidator.validatePassword(request.password());

            // 2-2. CustomPasswordEncoder를 통한 암호화 (pepper 포함)
            String encodedPassword = passwordEncoder.encode(request.password());
            user.updatePassword(encodedPassword);

            // 2-3. 비밀번호 변경 시, 토큰 삭제 (강제 로그아웃) -> 다시 로그인 해야함
            refreshTokenRepository.deleteByUser(user);
        }
        return MyPageResponseDTO.from(user);
    }

    // 회원 탈퇴 로직
    @Transactional
    public void deleteUser(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AuthException(AuthErrorCode.USER_NOT_FOUND));

        // 탈퇴 시 토큰 먼저 삭제
        refreshTokenRepository.deleteByUser(user);
        // 이후 사용자 계정 삭제
        userRepository.delete(user);
    }
}
