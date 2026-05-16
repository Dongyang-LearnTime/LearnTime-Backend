package learntime.backend.domain.study.service.core;

import learntime.backend.domain.quiz.repository.QuizHistoryRepository;
import learntime.backend.domain.study.converter.StudyConverter;
import learntime.backend.domain.study.dto.StudyDailyPlanStatsDTO;
import learntime.backend.domain.study.dto.response.StudyMemberRecentWeekInfoResponseDTO;
import learntime.backend.domain.study.dto.response.StudyRecentWeekInfoResponseDTO;
import learntime.backend.domain.study.dto.response.StudyTotalInfoResponseDTO;
import learntime.backend.domain.study.enums.CompletionStatus;
import learntime.backend.domain.study.enums.ProgressStatus;
import learntime.backend.domain.study.error.code.StudyErrorCode;
import learntime.backend.domain.study.error.exception.StudyException;
import learntime.backend.domain.study.model.*;
import learntime.backend.domain.study.repository.StudyDailyPlanRepository;
import learntime.backend.domain.study.repository.StudyRepository;
import learntime.backend.domain.study.repository.StudyRestDateRepository;
import learntime.backend.domain.study.repository.StudyRestDayRepository;
import learntime.backend.domain.study.repository.StudyStatusRepository;
import learntime.backend.global.error.code.ErrorCode;
import learntime.backend.global.error.exception.BusinessException;
import learntime.backend.global.utils.AuthorizationUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import java.time.DayOfWeek;
import java.time.LocalDate;
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

        if (stats.isEmpty()) {
            return buildEmptyIndicatorResponse();
        }

        return buildIndicatorResponse(studyMemberId, stats);
    }

    private StudyTotalInfoResponseDTO buildIndicatorResponse(Long targetId, List<StudyDailyPlanStatsDTO> stats) {
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
        
        // 퀴즈 통계를 멤버 기반 또는 스터디 전체 기반으로 조회
        Double quizCorrectRate = calculateQuizCorrectRate(targetId);

        return StudyTotalInfoResponseDTO.builder()
                .studyCompletionRate(completionRate)
                .studySuccessRate(successRate)
                .quizCorrectRate(quizCorrectRate)
                .totalFocusedTime(totalFocusedTime)
                .build();
    }

    // 오늘을 제외한 최근 7일의 날짜별 공부 상태를 모든 스터디 멤버에 대해 조회합니다.
    @Transactional(readOnly = true)
    public List<StudyMemberRecentWeekInfoResponseDTO> getRecentWeekStudyInfos(Long studyId, Long userId) {
        Study study = studyRepository.findById(studyId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_INPUT_VALUE));

        LocalDate today = LocalDate.now();
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

    // 스터디와 관련된 퀴즈의 전체 정답률을 계산함
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

