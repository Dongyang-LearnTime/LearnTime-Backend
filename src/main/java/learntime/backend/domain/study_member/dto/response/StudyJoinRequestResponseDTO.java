package learntime.backend.domain.study_member.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import learntime.backend.domain.study_member.enums.StudyJoinRequestStatus;

import java.time.LocalDateTime;

public record StudyJoinRequestResponseDTO(
        @Schema(description = "가입 요청 ID", example = "1")
        Long studyJoinRequestId,

        @Schema(description = "스터디 ID", example = "1")
        Long studyId,

        @Schema(description = "스터디 제목", example = "이펙티브 자바 스터디")
        String studyTitle,

        @Schema(description = "요청 사용자 ID", example = "2")
        Long requesterUserId,

        @Schema(description = "요청 사용자 닉네임", example = "열공러")
        String requesterName,

        @Schema(description = "가입 요청 상태", example = "PENDING")
        StudyJoinRequestStatus status,

        @Schema(description = "요청 일시")
        LocalDateTime createdAt
) {
}
