package learntime.backend.domain.study.service.core;

import learntime.backend.domain.quiz.repository.QuizHistoryRepository;
import learntime.backend.domain.study.converter.StudyConverter;
import learntime.backend.domain.study.dto.StudyDailyPlanStatsDTO;
import learntime.backend.domain.study.dto.response.*;
import learntime.backend.domain.study.enums.CompletionStatus;
import learntime.backend.domain.study.enums.ProgressStatus;
import learntime.backend.domain.study.enums.StudyPlanStatus;
import learntime.backend.domain.study.error.code.StudyErrorCode;
import learntime.backend.domain.study.error.exception.StudyException;
import learntime.backend.domain.study.model.*;
import learntime.backend.domain.study.repository.*;
import learntime.backend.global.error.code.ErrorCode;
import learntime.backend.global.error.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StudyQueryService {

    private final StudyRepository studyRepository;
    private final StudyDailyPlanRepository studyDailyPlanRepository;
    private final StudyRestDateRepository studyRestDateRepository;
    private final StudyRestDayRepository studyRestDayRepository;
    private final StudyStatusRepository studyStatusRepository;
    private final QuizHistoryRepository quizHistoryRepository;

    public StudyStatusResponseDTO getStudyStatus(Long studyId) {
        Study study = studyRepository.findById(studyId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_INPUT_VALUE));
        return StudyStatusResponseDTO.builder()
                .studyId(study.getStudyId())
                .status(study.getStatus())
                .build();
    }

    /** AI 분석에 필요한 종합 데이터를 생성합니다. */
    @Transactional(readOnly = true)
    public StudyAnalysisDataDTO getStudyAnalysisData(Long studyId, Long userId) {
        Study study = studyRepository.findById(studyId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_INPUT_VALUE));

        StudyMember member = study.getStudyMembers().stream()
                .filter(m -> m.getUser().getUserId().equals(userId))
                .findFirst()
                .orElseThrow(() -> new StudyException(StudyErrorCode.STUDY_MEMBER_NOT_FOUND));

        StudyTotalInfoResponseDTO indicator = getStudyMemberTotalIndicatorByUserId(studyId, userId);

        List<StudyStatus> recentStatuses = studyStatusRepository.findCompletedStatusByStudyMemberId(member.getStudyMemberId());

        List<StudyAnalysisDataDTO.DailyTopicStats> topicStats = recentStatuses.stream()
                .map(s -> StudyAnalysisDataDTO.DailyTopicStats.builder()
                        .topicContent(s.getStudyDailyPlan().getPlanContent())
                        .completionStatus(s.getCompletionStatus() != null ? s.getCompletionStatus().name() : "FAILURE")
                        .understandingScore(s.getUnderstandingScore())
                        .build())
                .toList();

        return StudyAnalysisDataDTO.builder()
                .studyCompletionRate(indicator.studyCompletionRate())
                .studySuccessRate(indicator.studySuccessRate())
                .totalFocusedTime(indicator.totalFocusedTime())
                .topicStats(topicStats)
                .build();
    }

    // 특정 유저의 전체 통계 지표를 조회합니다.
    @Transactional(readOnly = true)
    public StudyTotalInfoResponseDTO getStudyMemberTotalIndicatorByUserId(Long studyId, Long userId) {
        Study study = studyRepository.findById(studyId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_INPUT_VALUE));

        Long studyMemberId = study.getStudyMembers().stream()
                .filter(m -> m.getUser().getUserId().equals(userId))
                .map(StudyMember::getStudyMemberId)
                .findFirst()
                .orElseThrow(() -> new StudyException(StudyErrorCode.STUDY_MEMBER_NOT_FOUND));

        List<StudyDailyPlanStatsDTO> stats = studyStatusRepository.findStatsByStudyMemberId(studyMemberId);

        long totalPlans = study.getStudyDailyPlans().size();

        if (totalPlans == 0) {
            return buildEmptyIndicatorResponse();
        }

        return buildIndicatorResponse(studyMemberId, stats, totalPlans);
    }

    private StudyTotalInfoResponseDTO buildIndicatorResponse(Long studyMemberId, List<StudyDailyPlanStatsDTO> stats, long totalPlans) {
        Double completionRate = calculateRate(
                stats.stream().filter(s -> s.progressStatus() == ProgressStatus.COMPLETED).count(),
                totalPlans
        );
        Double successRate = calculateRate(
                stats.stream().filter(s -> s.completionStatus() == CompletionStatus.SUCCESS).count(),
                totalPlans
        );

        Long totalFocusedTime = calculateTotalFocusedTime(stats);
        
        Double quizCorrectRate = calculateQuizCorrectRate(studyMemberId);

        return StudyTotalInfoResponseDTO.builder()
                .studyCompletionRate(completionRate)
                .studySuccessRate(successRate)
                .quizCorrectRate(quizCorrectRate)
                .totalFocusedTime(totalFocusedTime)
                .build();
    }

    @Transactional(readOnly = true)
    public List<StudyMemberRecentWeekInfoResponseDTO> getRecentWeekStudyInfos(Long studyId, Long userId) {
        Study study = studyRepository.findById(studyId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_INPUT_VALUE));

        LocalDate today = LocalDate.now(java.util.TimeZone.getTimeZone("Asia/Seoul").toZoneId());
        List<StudyDailyPlan> recentPlans = studyDailyPlanRepository.findByStudyIdAndPlanDateBetweenOrderByPlanDateAsc(
                studyId,
                today.minusDays(7),
                today.minusDays(1)
        );

        Set<DayOfWeek> restDays = studyRestDayRepository.findAllByStudy_StudyId(studyId)
                .stream()
                .map(StudyRestDay::getDayOfWeek)
                .collect(Collectors.toSet());

        Set<LocalDate> restDates = studyRestDateRepository.findAllByStudy_StudyId(studyId)
                .stream()
                .map(StudyRestDate::getRestDate)
                .collect(Collectors.toSet());

        return study.getStudyMembers().stream()
                .map(member -> {
                    List<StudyStatus> statuses = studyStatusRepository.findByMemberIdAndPlanDateBetween(
                            member.getStudyMemberId(),
                            today.minusDays(7),
                            today.minusDays(1)
                    );
                    List<StudyRecentWeekInfoResponseDTO> memberRecentWeekInfos = StudyConverter.toRecentWeekStudyInfoResponseDTOs(recentPlans, statuses, today, restDays, restDates);
                    return new StudyMemberRecentWeekInfoResponseDTO(member.getStudyMemberId(), memberRecentWeekInfos);
                })
                .collect(Collectors.toList());
    }

    private StudyTotalInfoResponseDTO buildEmptyIndicatorResponse() {
        return StudyTotalInfoResponseDTO.builder()
                .studyCompletionRate(0.0)
                .studySuccessRate(0.0)
                .quizCorrectRate(null)
                .totalFocusedTime(null)
                .build();
    }

    private Double calculateRate(long targetCount, long totalCount) {
        if (totalCount == 0) return 0.0;
        return Math.round((double) targetCount / totalCount * 10000) / 100.0;
    }

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

    private Double calculateQuizCorrectRate(Long studyMemberId) {
        List<Object[]> quizStats = quizHistoryRepository.findQuizStatsByStudyMemberId(studyMemberId);

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
