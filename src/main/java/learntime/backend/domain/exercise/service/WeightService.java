package learntime.backend.domain.exercise.service;

import learntime.backend.domain.exercise.converter.ExerciseConverter;
import learntime.backend.domain.exercise.dto.request.WeightRequestDTO;
import learntime.backend.domain.exercise.dto.response.WeightResponseDTO;
import learntime.backend.domain.exercise.model.WeightRecord;
import learntime.backend.domain.exercise.repository.WeightRecordRepository;
import learntime.backend.domain.user.model.User;
import learntime.backend.domain.user.repository.UserRepository;
import learntime.backend.global.error.code.AuthErrorCode;
import learntime.backend.global.error.exception.AuthException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class WeightService {
    private final WeightRecordRepository weightRecordRepository;
    private final UserRepository userRepository;

    @Transactional
    public WeightResponseDTO saveWeight(Long userId, WeightRequestDTO request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthException(AuthErrorCode.USER_NOT_FOUND));

        WeightRecord record = ExerciseConverter.toWeightRecord(user, request);
        WeightRecord saved = weightRecordRepository.save(record);

        return ExerciseConverter.toWeightResponseDTO(saved);
    }
}
