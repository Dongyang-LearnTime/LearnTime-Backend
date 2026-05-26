package learntime.backend.domain.study.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.util.List;

@Builder
@Schema(description = "스터디 멤버의 작성 내용 응답 DTO")
public record StudyMemberContentResponseDTO(
        @Schema(description = "일일 계획 ID")
        Long studyDailyPlanId,

        @Schema(description = "일일 진도 내용")
        String planContent,

        @Schema(description = "오늘 휴무일 여부")
        Boolean isHoliday,

        List<memberContent> memberContents
) {
        @Builder
        public record memberContent(
                @Schema(description = "스터디 멤버 작성 내용 ID")
                Long studyMemberContentId,

                @Schema(description = "작성한 공부 내용")
                String memberContent
        ) {}
}
