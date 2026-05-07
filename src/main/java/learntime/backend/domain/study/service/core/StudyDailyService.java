package learntime.backend.domain.study.service.core;

import learntime.backend.domain.point.dto.PointEventDTO;
import learntime.backend.domain.point.enums.PointPolicy;
import learntime.backend.domain.point.enums.PointType;
import learntime.backend.domain.study.dto.request.PlanCompleteRequestDTO;
import learntime.backend.domain.study.dto.response.StudyDailyPlanInfoResponseDTO;
import learntime.backend.domain.study.enums.CompletionStatus;
import learntime.backend.domain.study.enums.ProgressStatus;
import learntime.backend.domain.study.error.code.StudyErrorCode;
import learntime.backend.domain.study.error.exception.StudyException;
import learntime.backend.domain.study.model.Study;
import learntime.backend.domain.study.model.StudyDailyPlan;
import learntime.backend.domain.study.repository.StudyDailyPlanRepository;
import learntime.backend.domain.study.repository.StudyRepository;
import learntime.backend.domain.study.repository.StudyRestDateRepository;
import learntime.backend.domain.study.repository.StudyRestDayRepository;
import learntime.backend.global.utils.AuthorizationUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import learntime.backend.domain.study.model.StudyRestDay;
import learntime.backend.domain.study.model.StudyRestDate;
import learntime.backend.domain.study.converter.StudyConverter;

// 일일 진도 및 포인트 지급 관련 비즈니스 로직 담당 서비스
@Service
@RequiredArgsConstructor
public class StudyDailyService {

    private final StudyDailyPlanRepository studyDailyPlanRepository;
    private final StudyRestDateRepository studyRestDateRepository;
    private final StudyRestDayRepository studyRestDayRepository;
    private final StudyRepository studyRepository;
    private final ApplicationEventPublisher eventPublisher;

    private static final int UNDERSTANDING_SCORE_WEIGHT = 2; // 이해도에 따른 가중치 (이해도 2면 10*2)

    @Transactional(readOnly = true)
    public StudyDailyPlanInfoResponseDTO getStudyPlanInfoByDate(Long studyId, LocalDate planDate, Long userId) {
        Study study = studyRepository.findById(studyId)
                .orElseThrow(() -> new StudyException(StudyErrorCode.STUDY_DAILY_NOT_FOUND));

        AuthorizationUtil.verifyOwnership(userId, study.getUser().getUserId());

        List<DayOfWeek> restDays = studyRestDayRepository.findAllByStudy_StudyId(studyId)
                .stream().map(StudyRestDay::getDayOfWeek).toList();

        List<LocalDate> restDates = studyRestDateRepository.findAllByStudy_StudyId(studyId)
                .stream().map(StudyRestDate::getRestDate).toList();

        StudyDailyPlan studyDailyPlan = studyDailyPlanRepository.findByStudyIdAndPlanDate(studyId, planDate)
                .orElse(null);

        return StudyConverter.toStudyDailyPlanInfoResponseDTO(planDate, study, restDays, restDates, studyDailyPlan);
    }

    @Transactional
    public int completeStudyDailyPlan(PlanCompleteRequestDTO request, Long userId) {
        StudyDailyPlan studyDailyPlan = studyDailyPlanRepository.findById(request.studyDailyPlanId())
                .orElseThrow(() -> new IllegalArgumentException("공부 일일 진도를 찾을 수 없습니다."));

        AuthorizationUtil.verifyOwnership(userId, studyDailyPlan.getStudy().getUser().getUserId());

        studyDailyPlan.setProgressStatus(ProgressStatus.COMPLETED);
        studyDailyPlan.setCompletionStatus(request.completionStatus());
        studyDailyPlan.setCompletionDate(LocalDateTime.now());

        int calculatedPoint = calculatePoint(request.completionStatus(), request.understandingScore());
        String description = determineDescription(request.completionStatus(), request.understandingScore());

        eventPublisher.publishEvent(new PointEventDTO(
                userId,
                calculatedPoint,
                PointType.EARN,
                description
        ));

        return calculatedPoint;
    }

    private int calculatePoint(CompletionStatus status, int understandingScore) {
        if (status == CompletionStatus.SUCCESS) {
            int bonus = understandingScore * UNDERSTANDING_SCORE_WEIGHT;
            return PointPolicy.STUDY_COMPLETED_SUCCESS.getAmount() + bonus;
        }
        return PointPolicy.STUDY_COMPLETED_FAILURE.getAmount();
    }

    private String determineDescription(CompletionStatus status, int understandingScore) {
        if (status == CompletionStatus.SUCCESS) {
            return String.format("%s (이해도: %d점)",
                    PointPolicy.STUDY_COMPLETED_SUCCESS.getDescription(), understandingScore);
        }
        return PointPolicy.STUDY_COMPLETED_FAILURE.getDescription();
    }

}
