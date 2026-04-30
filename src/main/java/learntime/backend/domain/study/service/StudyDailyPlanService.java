package learntime.backend.domain.study.service;

import jakarta.transaction.Transactional;
import learntime.backend.domain.point.dto.PointEventDTO;
import learntime.backend.domain.point.enums.PointPolicy;
import learntime.backend.domain.point.enums.PointType;
import learntime.backend.domain.study.dto.request.PlanCompleteRequestDTO;
import learntime.backend.domain.study.enums.CompletionStatus;
import learntime.backend.domain.study.enums.ProgressStatus;
import learntime.backend.domain.study.model.StudyDailyPlan;
import learntime.backend.domain.study.repository.StudyDailyPlanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class StudyDailyPlanService {

    private final StudyDailyPlanRepository studyDailyPlanRepository;
    private final ApplicationEventPublisher eventPublisher; // 스프링 이벤트 퍼블리셔 주입

    private static final int UNDERSTANDING_SCORE_WEIGHT = 2;

    @Transactional
    public void completeStudyDailyPlan(PlanCompleteRequestDTO request, Long userId) {
        StudyDailyPlan studyDailyPlan = studyDailyPlanRepository.findById(request.studyDailyPlanId())
                .orElseThrow(() -> new IllegalArgumentException("공부 일일 진도를 찾을 수 없습니다."));

        // 공부 일일 진도 상태 변경
        studyDailyPlan.setProgressStatus(ProgressStatus.COMPLETED); // 성공으로 전환
        studyDailyPlan.setCompletionStatus(request.completionStatus());
        studyDailyPlan.setCompletionDate(LocalDateTime.now()); // 완료일 생성

        // 지급 포인트 및 내역 상세 내용
        int calculatedPoint = calculatePoint(request.completionStatus(), request.understandingScore());
        String description = determineDescription(request.completionStatus(), request.understandingScore());

        // 이벤트 리스너로 포인트 등록
        eventPublisher.publishEvent(new PointEventDTO(
                userId,
                calculatedPoint,
                PointType.EARN,
                description
        ));
    }

    private int calculatePoint(CompletionStatus status, int understandingScore) {
        // 성공 시: 기본 점수 + (이해도 점수 * 가중치)
        if (status == CompletionStatus.SUCCESS) {
            int bonus = understandingScore * UNDERSTANDING_SCORE_WEIGHT;
            return PointPolicy.STUDY_COMPLETED_SUCCESS.getAmount() + bonus;
        }
        // 실패 시: 기본 격려 점수만 부여
        return PointPolicy.STUDY_COMPLETED_FAILURE.getAmount();
    }

    private String determineDescription(CompletionStatus status, int understandingScore) {
        // 출력 예시: "일일 진도 완료 (성공) (이해도: 4점)"
        if (status == CompletionStatus.SUCCESS) {
            return String.format("%s (이해도: %d점)",
                    PointPolicy.STUDY_COMPLETED_SUCCESS.getDescription(), understandingScore);
        }
        // 출력 예시: "일일 진도 완료 (실패/격려)"
        return PointPolicy.STUDY_COMPLETED_FAILURE.getDescription();
    }

}
