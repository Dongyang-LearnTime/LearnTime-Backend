package learntime.backend.domain.exercise.service;

import learntime.backend.domain.exercise.converter.ExerciseConverter;
import learntime.backend.domain.exercise.dto.request.WeightRequestDTO;
import learntime.backend.domain.exercise.dto.response.WeightResponseDTO;
import learntime.backend.domain.exercise.error.code.ExerciseErrorCode;
import learntime.backend.domain.exercise.error.exception.ExerciseException;
import learntime.backend.domain.exercise.model.WeightRecord;
import learntime.backend.domain.exercise.repository.WeightRecordRepository;
import learntime.backend.domain.user.model.User;
import learntime.backend.domain.user.repository.UserRepository;
import learntime.backend.global.error.code.AuthErrorCode;
import learntime.backend.global.error.exception.AuthException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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

    @Transactional(readOnly = true)
    public List<WeightResponseDTO> getRecentWeights(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthException(AuthErrorCode.USER_NOT_FOUND));

        return weightRecordRepository.findAllByUserOrderByCreatedAtDesc(user).stream()
                .map(ExerciseConverter::toWeightResponseDTO)
                .toList();
    }

    @Transactional
    public void deleteWeight(Long userId, Long weightRecordId) {
        WeightRecord record = weightRecordRepository.findById(weightRecordId)
                .orElseThrow(() -> new ExerciseException(ExerciseErrorCode.WEIGHT_RECORD_NOT_FOUND));

        if (!record.getUser().getUserId().equals(userId)) {
            throw new ExerciseException(ExerciseErrorCode.ACCESS_DENIED_WEIGHT);
        }

        weightRecordRepository.delete(record);
    }
}
