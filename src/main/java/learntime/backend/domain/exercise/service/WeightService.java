package learntime.backend.domain.exercise.service;

import learntime.backend.domain.exercise.dto.request.WeightRequestDTO;
import learntime.backend.domain.exercise.entity.WeightRecord;
import learntime.backend.domain.exercise.repository.WeightRecordRepository;
import learntime.backend.domain.user.model.User;
import learntime.backend.domain.user.repository.UserRepository;
import learntime.backend.global.error.BusinessException;
import learntime.backend.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class WeightService {
    private final WeightRecordRepository weightRecordRepository;
    private final UserRepository userRepository;

    @Transactional
    public WeightRecord saveWeight(String email, WeightRequestDTO request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        WeightRecord record = WeightRecord.builder()
                .user(user)
                .weight(request.getWeight())
                .bodyFat(request.getBodyFat())
                .build();

        return weightRecordRepository.save(record);
    }
}
