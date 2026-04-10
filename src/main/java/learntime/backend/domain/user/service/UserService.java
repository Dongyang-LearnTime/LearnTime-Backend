package learntime.backend.domain.user.service;

import learntime.backend.domain.study.repository.StudyRepository;
import learntime.backend.domain.user.model.User;
import learntime.backend.domain.user.repository.PromptQuotaRepository;
import learntime.backend.domain.user.repository.UserRepository;
import learntime.backend.global.error.code.AuthErrorCode;
import learntime.backend.global.error.exception.AuthException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final StudyRepository studyRepository;
    private final PromptQuotaRepository promptQuotaRepository;

    @Transactional
    public void deleteUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthException(AuthErrorCode.USER_NOT_FOUND));

        // 하위 테이블 soft delete
        studyRepository.softDeleteAllByUserId(userId);
        promptQuotaRepository.softDeleteByUserId(userId);

        userRepository.delete(user);
    }
}