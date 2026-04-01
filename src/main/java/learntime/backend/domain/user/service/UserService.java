package learntime.backend.domain.user.service;

import learntime.backend.domain.study.repository.StudyRepository;
import learntime.backend.domain.user.model.User;
import learntime.backend.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final StudyRepository studyRepository;

    @Transactional
    public void deleteUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        studyRepository.deleteAllByUserIdInBatch(userId); // 연관된 Study (및 하위 엔티티) 삭제
        userRepository.delete(user);
    }
}