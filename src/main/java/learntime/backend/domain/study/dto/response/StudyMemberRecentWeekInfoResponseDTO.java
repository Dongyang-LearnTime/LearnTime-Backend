package learntime.backend.domain.study.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "스터디 멤버의 최근 일주일 일일 학습 상태 응답 DTO")
public record StudyMemberRecentWeekInfoResponseDTO(
        @Schema(description = "스터디 멤버 ID")
        Long studyMemberId,
        
        @Schema(description = "해당 멤버의 최근 일주일 상태 목록")
        List<StudyRecentWeekInfoResponseDTO> recentWeekInfos
) {
}
