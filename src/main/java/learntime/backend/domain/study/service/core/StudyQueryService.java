package learntime.backend.domain.study.service.core;

import learntime.backend.domain.quiz.repository.QuizHistoryRepository;
import learntime.backend.domain.study.converter.StudyConverter;
import learntime.backend.domain.study.dto.StudyDailyPlanStatsDTO;
import learntime.backend.domain.study.dto.response.*;
import learntime.backend.domain.study.enums.CompletionStatus;
import learntime.backend.domain.study.enums.ProgressStatus;
import learntime.backend.domain.study.error.code.StudyErrorCode;
import learntime.backend.domain.study.error.exception.StudyException;
import learntime.backend.domain.study.model.*;
import learntime.backend.domain.study.repository.*;
import learntime.backend.domain.study_member.enums.StudyMemberStatus;
import learntime.backend.domain.study_member.model.StudyMember;
import learntime.backend.domain.study_member.repository.StudyMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.cache.annotation.Cacheable;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.*;
import java.util.TimeZone;
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
    private final StudyMemberRepository studyMemberRepository;

    public StudyStatusResponseDTO getStudyStatus(Long studyId) {
        Study study = studyRepository.findById(studyId)
                .orElseThrow(() -> new StudyException(StudyErrorCode.STUDY_NOT_FOUND));
        return StudyStatusResponseDTO.builder()
                .studyId(study.getStudyId())
                .status(study.getStatus())
                .build();
    }

    /** AI 분석에 필요한 종합 데이터를 생성합니다. */
    @Transactional(readOnly = true)
    public StudyAnalysisDataDTO getStudyAnalysisData(Long studyId, Long userId) {
        Study study = studyRepository.findById(studyId)
                .orElseThrow(() -> new StudyException(StudyErrorCode.STUDY_NOT_FOUND));

        // Fix 4: Lazy 컬렉션 스트림 대신 repository 직접 조회로 NPE 방지
        StudyMember member = studyMemberRepository
                .findByStudy_StudyIdAndUser_UserIdAndStatus(studyId, userId, StudyMemberStatus.ACTIVE)
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
    @Cacheable(value = "studyTotalIndicator", key = "#studyId + '_' + #userId")
    public StudyTotalInfoResponseDTO getStudyMemberTotalIndicatorByUserId(Long studyId, Long userId) {
        Long studyMemberId = studyMemberRepository.findActiveStudyMemberIdByStudyIdAndUserId(studyId, userId)
                .orElseThrow(() -> {
                    if (!studyRepository.existsById(studyId)) {
                        return new StudyException(StudyErrorCode.STUDY_NOT_FOUND);
                    }
                    return new StudyException(StudyErrorCode.STUDY_MEMBER_NOT_FOUND);
                });

        List<StudyDailyPlanStatsDTO> stats = studyStatusRepository.findStatsByStudyMemberId(studyMemberId);

        long totalPlans = studyDailyPlanRepository.countByStudy_StudyId(studyId);

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
    @Cacheable(value = "recentWeekStudyIndicator", key = "#studyId")
    public List<StudyMemberRecentWeekInfoResponseDTO> getRecentWeekStudyInfos(Long studyId) {
        Study study = studyRepository.findByIdWithStudyMembersAndUser(studyId)
                .orElseThrow(() -> new StudyException(StudyErrorCode.STUDY_NOT_FOUND));

        LocalDate today = LocalDate.now(TimeZone.getTimeZone("Asia/Seoul").toZoneId());
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

        List<StudyMember> studyMembers = study.getStudyMembers().stream()
                .filter(StudyMember::isActive)
                .toList();
        List<Long> studyMemberIds = studyMembers.stream()
                .map(StudyMember::getStudyMemberId)
                .toList();

        List<StudyStatus> allStatuses = studyStatusRepository.findByStudyMemberIdInAndPlanDateBetween(
                studyMemberIds,
                today.minusDays(7),
                today.minusDays(1)
        );

        Map<Long, List<StudyStatus>> statusMap = allStatuses.stream()
                .collect(Collectors.groupingBy(status -> status.getStudyMember().getStudyMemberId()));

        return studyMembers.stream()
                .map(member -> {
                    List<StudyStatus> statuses = statusMap.getOrDefault(member.getStudyMemberId(), List.of());
                    return StudyConverter.toStudyMemberRecentWeekInfoResponseDTO(member, recentPlans, statuses, today, restDays, restDates);
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

    @Transactional(readOnly = true)
    public List<StudyProgressIndicatorResponseDTO> getMyStudyProgresses(Long userId) {
        List<StudyMember> activeMembers = studyMemberRepository.findAllActiveByUserIdFetchStudy(userId);
        if (activeMembers.isEmpty()) {
            return List.of();
        }

        List<Long> activeStudyIds = activeMembers.stream()
                .map(sm -> sm.getStudy().getStudyId())
                .toList();

        LocalDate today = LocalDate.now(TimeZone.getTimeZone("Asia/Seoul").toZoneId());
        List<Long> studyIdsWithTodayPlan = studyDailyPlanRepository.findStudyIdsWithPlanDate(activeStudyIds, today);
        Set<Long> studyIdsWithTodayPlanSet = new HashSet<>(studyIdsWithTodayPlan);

        return activeMembers.stream()
                .map(sm -> StudyConverter.toStudyProgressIndicatorResponseDTO(
                        sm.getStudy().getStudyId(),
                        sm.getStudy().getStudyTitle(),
                        studyIdsWithTodayPlanSet.contains(sm.getStudy().getStudyId())
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public String getStudyTitle(Long studyId) {
        return studyRepository.findById(studyId)
                .map(Study::getStudyTitle)
                .orElseThrow(() -> new StudyException(StudyErrorCode.STUDY_NOT_FOUND));
    }
}
