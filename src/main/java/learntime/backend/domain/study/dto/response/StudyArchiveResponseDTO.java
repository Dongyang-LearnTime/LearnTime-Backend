package learntime.backend.domain.study.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import learntime.backend.domain.study_member.enums.StudyMemberRole;
import learntime.backend.domain.study_member.enums.StudyMemberStatus;
import lombok.Builder;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Builder
@Schema(description = "마이페이지 아카이브 — 참여 이력 스터디 응답 DTO")
public record StudyArchiveResponseDTO(

        @Schema(description = "스터디 ID")
        Long studyId,

        @Schema(description = "스터디 제목")
        String studyTitle,

        @Schema(description = "교재 제목")
        String bookTitle,

        @Schema(description = "스터디 시작일")
        LocalDate startDate,

        @Schema(description = "스터디 종료일")
        LocalDate endDate,

        @Schema(description = "내 역할 (OWNER / MEMBER)")
        StudyMemberRole myRole,

        @Schema(description = "내 현재 상태 (ACTIVE / WITHDRAWN)")
        StudyMemberStatus myStatus,

        @Schema(description = "스터디 참여 일시")
        LocalDateTime joinedAt
) {
}
