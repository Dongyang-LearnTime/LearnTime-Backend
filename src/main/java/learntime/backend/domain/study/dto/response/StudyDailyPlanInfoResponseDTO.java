package learntime.backend.domain.study.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import learntime.backend.domain.study.enums.CompletionStatus;
import learntime.backend.domain.study.enums.ProgressStatus;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Schema(description = "일일 학습 계획 상세 및 스터디 전반 휴무 정보 응답 DTO")
public record StudyDailyPlanInfoResponseDTO(
        @Schema(description = "조회 기준일")
        LocalDate planDate,

        @Schema(description = "스터디 시작일")
        LocalDate startDate,

        @Schema(description = "스터디 종료일")
        LocalDate endDate,

        @Schema(description = "스터디 휴무 요일 목록")
        List<DayOfWeek> restDays,

        @Schema(description = "스터디 휴무 날짜 목록")
        List<LocalDate> restDates,

        @Schema(description = "일일 학습 계획 ID (계획이 없으면 null)")
        Long studyDailyPlanId,

        @Schema(description = "학습 일차 (계획이 없으면 null)")
        Integer dayNumber,

        @Schema(description = "학습 계획 내용 (계획이 없으면 null)")
        String planContent,

        @Schema(description = "집중 시간 (계획이 없으면 null)", type = "string", pattern = "HH:mm:ss")
        LocalTime focusTime,

        @Schema(description = "진행 상태 (계획이 없으면 null)")
        ProgressStatus progressStatus,

        @Schema(description = "완료 상태 (계획이 없으면 null)")
        CompletionStatus completionStatus,

        @Schema(description = "이해도 점수 (계획이 없으면 null)")
        Integer understandingScore,

        @Schema(description = "조회한 사용자의 스터디 멤버 ID")
        Long studyMemberId,

        @Schema(description = "모든 스터디 멤버 ID 목록")
        List<Long> allStudyMemberIds
) {}
