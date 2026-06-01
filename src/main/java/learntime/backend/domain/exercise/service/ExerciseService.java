package learntime.backend.domain.exercise.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import learntime.backend.domain.badge.event.ExerciseCompletedEvent;
import learntime.backend.domain.exercise.converter.ExerciseConverter;
import learntime.backend.domain.exercise.dto.request.ExerciseRequestDTO;
import learntime.backend.domain.exercise.dto.response.ExerciseCalorieResponseDTO;
import learntime.backend.domain.exercise.event.ExerciseCalorieRequestEvent;
import learntime.backend.domain.exercise.dto.response.ExerciseCalorieResponseDTO;
import learntime.backend.domain.exercise.dto.response.ExerciseResponseDTO;
import learntime.backend.domain.exercise.dto.response.WeeklyWeightStatsResponseDTO;
import learntime.backend.domain.exercise.error.code.ExerciseErrorCode;
import learntime.backend.domain.exercise.error.exception.ExerciseException;
import learntime.backend.domain.exercise.model.ExerciseRecord;
import learntime.backend.domain.exercise.repository.ExerciseRecordRepository;
import learntime.backend.domain.user.model.User;
import learntime.backend.domain.user.repository.UserRepository;
import learntime.backend.global.common.GeminiModel;
import learntime.backend.global.error.code.AuthErrorCode;
import learntime.backend.global.error.exception.AuthException;
import learntime.backend.global.infra.gemini.GeminiClient;
import learntime.backend.global.infra.youtube.YoutubeClient;
import learntime.backend.global.dto.YoutubeVideoResponseDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
@Slf4j
public class ExerciseService {
    private final ExerciseRecordRepository exerciseRecordRepository;
    private final UserRepository userRepository;
    private final GeminiClient geminiClient;
    private final ObjectMapper objectMapper;
    private final YoutubeClient youtubeClient;
    private final ApplicationEventPublisher eventPublisher;
    private final ExercisePromptProvider promptProvider;

    public List<YoutubeVideoResponseDTO> getRecommendedVideos(List<String> bodyParts) {
        if (bodyParts == null || bodyParts.isEmpty()) {
            return youtubeClient.searchVideos("전신 홈 트레이닝");
        }

        List<YoutubeVideoResponseDTO> allVideos = new ArrayList<>();
        for (String part : bodyParts) {
            allVideos.addAll(youtubeClient.searchVideos(part));
        }
        return allVideos;
    }

    @Transactional(readOnly = true)
    public List<WeeklyWeightStatsResponseDTO> getRecentWeeklyWeightStats(Long userId) {
        User user = findUserByIdOrThrow(userId);
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime sevenDaysAgo = now.minusDays(7);

        // 최근 일주일 동안의 운동 정보 가져옴
        List<ExerciseRecord> exercises = exerciseRecordRepository.
                        findAllByUserAndCreatedAtBetweenOrderByCreatedAtAsc(user, sevenDaysAgo, now);

        // 날짜별 총 운동 무게 계산 (LinkedHashMap로 순서 유지, null이면 0.0이 들어감)
        Map<LocalDate, Double> dailyWeightMap = exercises.stream()
                        .collect(Collectors.groupingBy(exercise -> exercise.getCreatedAt().toLocalDate(),
                                LinkedHashMap::new, Collectors.summingDouble(exercise ->
                                        Optional.ofNullable(exercise.getWeight())
                                                .orElse(0.0)
                                )
                        ));

        // DTO 리스트에 넣어서 반환함
        return dailyWeightMap.entrySet()
                .stream()
                .map(entry ->
                        ExerciseConverter.toWeeklyWeightStatsResponseDTO(
                                entry.getValue(),
                                entry.getKey()
                        )
                )
                .toList();
    }

    @Transactional
    public ExerciseResponseDTO saveExercise(Long userId, ExerciseRequestDTO request) {
        User user = findUserByIdOrThrow(userId);

        // 칼로리 계산을 비동기로 미루기 위해 일단 null 처리하여 초기 저장
        ExerciseCalorieResponseDTO emptyCalories = new ExerciseCalorieResponseDTO(null);
        ExerciseRecord record = ExerciseConverter.toExerciseRecord(user, request, emptyCalories);
        ExerciseRecord savedRecord = exerciseRecordRepository.save(record);

        // 비동기 칼로리 계산 이벤트 발행
        eventPublisher.publishEvent(new ExerciseCalorieRequestEvent(savedRecord.getExerciseRecordId(), request));

        ExerciseResponseDTO result =
                ExerciseConverter.toExerciseResponseDTO(savedRecord);

        eventPublisher.publishEvent(new ExerciseCompletedEvent(userId, LocalDateTime.now()));
        return result;
    }

    @Transactional(readOnly = true)
    public List<ExerciseResponseDTO> getExercises(Long userId) {
        User user = findUserByIdOrThrow(userId);

        return exerciseRecordRepository.findAllByUserOrderByCreatedAtDesc(user).stream()
                .map(ExerciseConverter::toExerciseResponseDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public ExerciseResponseDTO getExercise(Long userId, Long exerciseRecordId) {
        ExerciseRecord record = exerciseRecordRepository.findById(exerciseRecordId)
                .orElseThrow(() -> new ExerciseException(ExerciseErrorCode.EXERCISE_RECORD_NOT_FOUND));

        if (!record.getUser().getUserId().equals(userId)) {
            throw new ExerciseException(ExerciseErrorCode.ACCESS_DENIED_EXERCISE);
        }
        return ExerciseConverter.toExerciseResponseDTO(record);
    }

    @Transactional
    public ExerciseResponseDTO updateExercise(Long userId, Long exerciseRecordId, ExerciseRequestDTO request) {
        ExerciseRecord record = exerciseRecordRepository.findById(exerciseRecordId)
                .orElseThrow(() -> new ExerciseException(ExerciseErrorCode.EXERCISE_RECORD_NOT_FOUND));

        if (!record.getUser().getUserId().equals(userId)) {
            throw new ExerciseException(ExerciseErrorCode.ACCESS_DENIED_EXERCISE);
        }

        boolean requireRecalculation = !record.getBodyParts().equals(request.getBodyParts()) ||
                !record.getDuration().equals(request.getDuration()) ||
                !record.getContent().equals(request.getContent());

        if (requireRecalculation) {
            record.updateRecord(request.getBodyParts(), request.getDuration(), request.getContent(), request.getWeight(), null);
            eventPublisher.publishEvent(new ExerciseCalorieRequestEvent(record.getExerciseRecordId(), request));
        } else {
            record.updateRecord(request.getBodyParts(), request.getDuration(), request.getContent(), request.getWeight(), record.getCalories());
        }

        return ExerciseConverter.toExerciseResponseDTO(record);
    }

    @Transactional
    public void deleteExercise(Long userId, Long exerciseRecordId) {
        ExerciseRecord record = exerciseRecordRepository.findById(exerciseRecordId)
                .orElseThrow(() -> new ExerciseException(ExerciseErrorCode.EXERCISE_RECORD_NOT_FOUND));

        if (!record.getUser().getUserId().equals(userId)) {
            throw new ExerciseException(ExerciseErrorCode.ACCESS_DENIED_EXERCISE);
        }

        exerciseRecordRepository.delete(record);
    }

    private User findUserByIdOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new AuthException(AuthErrorCode.USER_NOT_FOUND));
    }


}
