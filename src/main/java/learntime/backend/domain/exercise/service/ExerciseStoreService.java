package learntime.backend.domain.exercise.service;

import learntime.backend.domain.exercise.repository.ExerciseRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ExerciseStoreService {

    private final ExerciseRecordRepository exerciseRecordRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateCalories(Long exerciseRecordId, Integer calories) {
        exerciseRecordRepository.findById(exerciseRecordId).ifPresent(record -> {
            record.updateCalories(calories);
        });
    }
}
