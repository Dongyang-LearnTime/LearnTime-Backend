package learntime.backend.domain.study.service.core;

import learntime.backend.domain.quiz.repository.QuizHistoryRepository;
import learntime.backend.domain.study.converter.StudyConverter;
import learntime.backend.domain.study.dto.StudyDailyPlanStatsDTO;
import learntime.backend.domain.study.dto.response.StudyRecentWeekInfoResponseDTO;
import learntime.backend.domain.study.dto.response.StudyTotalInfoResponseDTO;
import learntime.backend.domain.study.enums.CompletionStatus;
import learntime.backend.domain.study.enums.ProgressStatus;
import learntime.backend.domain.study.model.Study;
import learntime.backend.domain.study.model.StudyDailyPlan;
import learntime.backend.domain.study.model.StudyRestDate;
import learntime.backend.domain.study.model.StudyRestDay;
import learntime.backend.domain.study.repository.StudyDailyPlanRepository;
import learntime.backend.domain.study.repository.StudyRepository;
import learntime.backend.domain.study.repository.StudyRestDateRepository;
import learntime.backend.domain.study.repository.StudyRestDayRepository;
import learntime.backend.global.error.code.ErrorCode;
import learntime.backend.global.error.exception.BusinessException;
import learntime.backend.global.utils.AuthorizationUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import learntime.backend.domain.study.dto.request.GeminiReplanRequestDTO;
import learntime.backend.domain.study.service.util.StudyDateCalculator;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class StudyQueryService {

    private final StudyRepository studyRepository;
    private final StudyDailyPlanRepository studyDailyPlanRepository;
    private final StudyRestDateRepository studyRestDateRepository;
    private final StudyRestDayRepository studyRestDayRepository;
    private final QuizHistoryRepository quizHistoryRepository;
    private final StudyDateCalculator studyDateCalculator;
    private final StudyShareService studyShareService;

    // 남은 학습 기간 중 휴일을 제외한 실제 학습 가능 일수를 계산합니다.
    @Transactional(readOnly = true)
    public int calculateRemainingStudyDays(Long studyId, GeminiReplanRequestDTO request, Long userId) {
        Study study = studyRepository.findById(studyId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_INPUT_VALUE));

        AuthorizationUtil.verifyOwnership(userId, study.getUser().getUserId());

        Set<DayOfWeek> restDays = request.restDays() == null
                ? Set.of()
                : Set.copyOf(request.restDays());

        Set<LocalDate> restDates = request.restDates() == null
                ? Set.of()
                : Set.copyOf(request.restDates());

        long completedCount = studyDailyPlanRepository.countCompletedPlansByStudyAndDateRange(
                study,
                ProgressStatus.COMPLETED,
                request.startDate(),
                request.endDate()
        );

        return studyDateCalculator.calculateRemainingDays(
                request.startDate(),
                request.endDate(),
                restDays,
                restDates,
                completedCount
        );
    }

    // 스터디의 사용자별 전체 통계 지표(달성률, 성공률, 퀴즈 정답률 등)를 조회합니다.
    @Transactional(readOnly = true)
    @Cacheable(value = "studyTotalIndicator", key = "#studyId + ':' + #userId")
    public StudyTotalInfoResponseDTO getStudyTotalIndicator(Long studyId, Long userId) {
        studyShareService.getActiveParticipant(studyId, userId);

        List<StudyDailyPlanStatsDTO> stats = studyDailyPlanRepository.findStatsByStudyIdAndUserId(studyId, userId);

        if (stats.isEmpty()) {
            return buildEmptyIndicatorResponse();
        }

        long totalPlans = stats.size();
        Double completionRate = calculateRate(
                stats.stream().filter(s -> s.progressStatus() == ProgressStatus.COMPLETED).count(),
                totalPlans
        );
        Double successRate = calculateRate(
                stats.stream().filter(s -> s.completionStatus() == CompletionStatus.SUCCESS).count(),
                totalPlans
        );

        Long totalFocusedTime = calculateTotalFocusedTime(stats);
        Double quizCorrectRate = calculateQuizCorrectRate(studyId);

        return StudyTotalInfoResponseDTO.builder()
                .studyCompletionRate(completionRate)
                .studySuccessRate(successRate)
                .quizCorrectRate(quizCorrectRate)
                .totalFocusedTime(totalFocusedTime)
                .build();
    }

    // 오늘을 제외한 최근 7일의 날짜별 공부 상태를 조회합니다.
    @Transactional(readOnly = true)
    @Cacheable(value = "studyRecentWeekInfo", key = "#studyId + ':' + #userId")
    public List<StudyRecentWeekInfoResponseDTO> getRecentWeekStudyInfos(Long studyId, Long userId) {
        studyShareService.getActiveParticipant(studyId, userId);

        LocalDate today = LocalDate.now();
        List<StudyDailyPlan> recentPlans = studyDailyPlanRepository.findByStudyIdAndUserIdAndPlanDateBetweenOrderByPlanDateAsc(
                studyId,
                userId,
                today.minusDays(7),
                today.minusDays(1)
        );

        Set<DayOfWeek> restDays = studyRestDayRepository.findAllByStudy_StudyId(studyId)
                .stream()
                .map(StudyRestDay::getDayOfWeek)
                .collect(java.util.stream.Collectors.toSet());

        Set<LocalDate> restDates = studyRestDateRepository.findAllByStudy_StudyId(studyId)
                .stream()
                .map(StudyRestDate::getRestDate)
                .collect(java.util.stream.Collectors.toSet());

        return StudyConverter.toRecentWeekStudyInfoResponseDTOs(recentPlans, today, restDays, restDates);
    }

    // 아직 완료되지 않은 학습 계획의 내용을 통합하여 문자열로 반환합니다.
    @Transactional(readOnly = true)
    public String getRemainingStudyContent(Long studyId, Long userId) {
        Study study = studyRepository.findById(studyId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_INPUT_VALUE));

        AuthorizationUtil.verifyOwnership(userId, study.getUser().getUserId());

        return String.join("\n", studyDailyPlanRepository.findRemainingContents(
                study,
                ProgressStatus.COMPLETED
        ));
    }

    // 데이터가 없는 경우를 위한 빈 통계 지표 응답 객체를 생성합니다.
    private StudyTotalInfoResponseDTO buildEmptyIndicatorResponse() {
        return StudyTotalInfoResponseDTO.builder()
                .studyCompletionRate(0.0)
                .studySuccessRate(0.0)
                .quizCorrectRate(null)
                .totalFocusedTime(null)
                .build();
    }

    // 전체 개수 대비 목표 개수의 비율을 백분율로 계산합니다.
    private Double calculateRate(long targetCount, long totalCount) {
        if (totalCount == 0) return 0.0;
        return Math.round((double) targetCount / totalCount * 10000) / 100.0;
    }

    // 전체 일일 계획에서 기록된 총 집중 시간을 초 단위로 계산합니다.
    private Long calculateTotalFocusedTime(List<StudyDailyPlanStatsDTO> stats) {
        long totalFocusSeconds = 0;
        boolean hasFocusTime = false;

        for (StudyDailyPlanStatsDTO stat : stats) {
            if (stat.focusTime() != null) {
                totalFocusSeconds += stat.focusTime().toSecondOfDay();
                hasFocusTime = true;
            }
        }

        return hasFocusTime ? totalFocusSeconds : null;
    }

    // 스터디와 관련된 퀴즈의 전체 정답률을 계산합니다.
    private Double calculateQuizCorrectRate(Long studyId) {
        List<Object[]> quizStats = quizHistoryRepository.findQuizStatsByStudyId(studyId);

        if (quizStats.isEmpty() || quizStats.getFirst()[0] == null) {
            return null;
        }

        long totalAnswers = ((Number) quizStats.getFirst()[0]).longValue();
        long correctAnswers = quizStats.getFirst()[1] != null ? ((Number) quizStats.getFirst()[1]).longValue() : 0;

        return totalAnswers > 0
                ? Math.round(((double) correctAnswers / totalAnswers) * 10000) / 100.0
                : null;
    }
}
