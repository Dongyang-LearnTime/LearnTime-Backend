package learntime.backend.domain.study.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "스터디 멤버의 작성 내용 응답 DTO")
public record StudyMemberContentResponseDTO(
        @Schema(description = "스터디 멤버 작성 내용 ID")
        Long studyMemberContentId,

        @Schema(description = "일일 계획 ID")
        Long studyDailyPlanId,

        @Schema(description = "학습 일차")
        Integer dayNumber,

        @Schema(description = "작성한 공부 내용")
        String memberContent
) {}
