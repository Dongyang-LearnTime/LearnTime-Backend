package learntime.backend.domain.study.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "study_daily_plan",
        indexes = {
                // 스터디의 전체 일정을 일차별로 조회 및 정렬 O(log N) 탐색 커버
                @Index(name = "idx_study_daily_plan_on_study_and_day", columnList = "study_id, day_number"),
                // 스터디 대시보드 등에서 '특정 상태(예: 진행 중)'의 계획만 필터링 조회할 때 풀 테이블 스캔 방지
                @Index(name = "idx_study_daily_plan_on_study_and_status", columnList = "study_id, progress_status")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StudyDailyPlan {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long studyDailyPlanId;

    @ManyToOne(fetch = FetchType.LAZY) // N+1 문제 방지를 위한 지연 로딩 필수
    @JoinColumn(name = "study_id", nullable = false)
    private Study study;

    @Column(name = "day_number", nullable = false)
    private int dayNumber; // DTO의 day

    @Column(name = "plan_content", nullable = false)
    private String planContent;

    // 진행 여부 (시작 전, 진행 중, 완료)
    @Enumerated(EnumType.STRING)
    @Column(name = "progress_status", nullable = false, length = 20)
    private ProgressStatus progressStatus;

    // 완료 여부 결과 (성공, 실패)
    @Enumerated(EnumType.STRING)
    @Column(name = "completion_status", length = 20)
    private CompletionStatus completionStatus;

    @Builder
    public StudyDailyPlan(Study study, int dayNumber, String planContent) {
        this.study = study;
        this.dayNumber = dayNumber;
        this.planContent = planContent;
        this.progressStatus = ProgressStatus.NOT_STARTED;
    }

    // --- 비즈니스 로직 --- //
    public void startPlan() {
        if (this.progressStatus != ProgressStatus.NOT_STARTED) {
            throw new IllegalStateException("이미 시작되었거나 종료된 계획입니다.");
        }
        this.progressStatus = ProgressStatus.IN_PROGRESS;
    }

    // 완료 상태에서의 최종 결과 (성공 or 실패)
    public void completePlan(CompletionStatus result) {
        this.progressStatus = ProgressStatus.COMPLETED;
        this.completionStatus = result;
    }

    public enum ProgressStatus {
        NOT_STARTED,  // 시작 전
        IN_PROGRESS,  // 진행 중
        COMPLETED     // 완료
    }

    public enum CompletionStatus {
        SUCCESS,      // 완료 (성공)
        FAILURE       // 실패 (미달성 등)
    }
}