package learntime.backend.domain.user.service;

import learntime.backend.domain.user.converter.UserConverter;
import learntime.backend.domain.user.dto.response.MyPageResponseDTO;
import learntime.backend.domain.user.model.User;
import learntime.backend.domain.user.repository.UserRepository;
import learntime.backend.global.security.CustomPasswordEncoder;
import learntime.backend.global.error.code.AuthErrorCode;
import learntime.backend.global.error.exception.AuthException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MyPageService {

    private final UserRepository userRepository;
    private final CustomPasswordEncoder passwordEncoder;
    private final AuthService authService;

    @Transactional(readOnly = true)
    public MyPageResponseDTO getMyInfo(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AuthException(AuthErrorCode.USER_NOT_FOUND));
        return UserConverter.toMyPageResponseDTO(user);
    }

    @Transactional
    public AuthService.TokenPair updateName(String email, String name) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AuthException(AuthErrorCode.USER_NOT_FOUND));
        if (userRepository.existsByName(name)) {
            throw new AuthException(AuthErrorCode.USER_NAME_DUPLICATED);
        }
        user.updateInfo(name);
        return authService.generateTokenPair(user);
    }

    @Transactional
    public void updatePassword(String email, String currentPassword, String newPassword) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AuthException(AuthErrorCode.USER_NOT_FOUND));
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new AuthException(AuthErrorCode.PASSWORD_NOT_MATCH);
        }
        user.updatePassword(passwordEncoder.encode(newPassword));
    }

}
