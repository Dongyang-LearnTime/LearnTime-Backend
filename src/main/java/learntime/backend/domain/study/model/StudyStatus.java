package learntime.backend.domain.study.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import learntime.backend.domain.study.enums.CompletionStatus;
import learntime.backend.domain.study.enums.ProgressStatus;
import learntime.backend.domain.study_member.model.StudyMember;
import learntime.backend.global.common.BaseTimeEntity;
import lombok.*;

import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(
        name = "study_status",
        indexes = {
                @Index(name = "idx_study_status_member_plan", columnList = "study_member_id, study_daily_plan_id"),
                @Index(name = "idx_study_status_member_progress", columnList = "study_member_id, progress_status")
        }
)
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StudyStatus extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long studyStatusId;

    // 진행 여부 (시작 전, 진행 중, 완료)
    @Enumerated(EnumType.STRING)
    @Column(name = "progress_status", nullable = false, length = 20)
    private ProgressStatus progressStatus;

    // 완료 여부 결과 (성공, 실패)
    @Enumerated(EnumType.STRING)
    @Column(name = "completion_status", length = 20)
    private CompletionStatus completionStatus;

    // 이해도 점수 (1~5점)
    @Column(name = "understanding_score", columnDefinition = "TINYINT")
    @Min(value = 1, message = "점수는 1점 이상이어야 합니다.")
    @Max(value = 5, message = "점수는 최대 5점까지 가능합니다.")
    private Integer understandingScore;

    @Column
    private LocalDateTime completionDate; // 완료 날짜

    // 집중 시간
    @Column
    private LocalTime focusTime;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "study_member_id", nullable = false)
    private StudyMember studyMember;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "study_daily_plan_id", nullable = false)
    private StudyDailyPlan studyDailyPlan;

    @Builder
    public StudyStatus(ProgressStatus progressStatus, CompletionStatus completionStatus, Integer understandingScore, LocalTime focusTime, StudyMember studyMember, StudyDailyPlan studyDailyPlan) {
        this.progressStatus = progressStatus != null ? progressStatus : ProgressStatus.NOT_STARTED;
        this.completionStatus = completionStatus;
        this.understandingScore = understandingScore;
        this.focusTime = focusTime;
        this.studyMember = studyMember;
        this.studyDailyPlan = studyDailyPlan;
    }


    // --- 비즈니스 로직 --- //
    public void startPlan() {
        if (this.progressStatus != ProgressStatus.NOT_STARTED) {
            throw new IllegalStateException("이미 시작되었거나 종료된 계획입니다.");
        }
        this.progressStatus = ProgressStatus.IN_PROGRESS;
    }

    // 완료 상태에서의 최종 결과 (성공 or 실패)
    public void completePlan(CompletionStatus result, Integer understandingScore) {
        if (understandingScore != null && (understandingScore < 1 || understandingScore > 5)) {
            throw new IllegalArgumentException("이해도는 1점에서 5점 사이의 정수여야 합니다.");
        }

        this.progressStatus = ProgressStatus.COMPLETED;
        this.completionStatus = result;
        this.understandingScore = understandingScore;
    }


}
