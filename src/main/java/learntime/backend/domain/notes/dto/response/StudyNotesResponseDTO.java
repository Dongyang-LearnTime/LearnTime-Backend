package learntime.backend.domain.notes.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import learntime.backend.domain.notes.model.StudyNotes;
import lombok.Builder;

import java.time.LocalDateTime;

@Schema(description = "공부 필기 응답 DTO")
@Builder
public record StudyNotesResponseDTO (
        @Schema(description = "공부 필기 ID")
        Long studyNotesId,
        
        @Schema(description = "공부 맴버 ID (없을 경우 null)")
        Long studyMemberId,
        
        @Schema(description = "필기 제목")
        String title,
        
        @Schema(description = "필기 내용")
        String content,
        
        @Schema(description = "생성일")
        LocalDateTime createdAt,
        
        @Schema(description = "수정일")
        LocalDateTime updatedAt
) { }
