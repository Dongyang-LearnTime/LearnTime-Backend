package learntime.backend.domain.study.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import learntime.backend.domain.study.enums.StudyParticipantRole;
import learntime.backend.domain.study.model.StudyParticipant;

import java.time.LocalDateTime;

@Schema(description = "스터디 참가자 정보 응답 DTO")
public record StudyParticipantResponseDTO(
        Long studyParticipantId,   // 참가자 관리 식별자
        Long userId,               // 사용자 식별자
        String name,               // 사용자 이름
        String email,              // 사용자 이메일
        StudyParticipantRole role, // 참가자 역할 (OWNER, MEMBER)
        LocalDateTime joinedAt     // 참가 일시
) {
    public static StudyParticipantResponseDTO from(StudyParticipant participant) {
        return new StudyParticipantResponseDTO(
                participant.getStudyParticipantId(),
                participant.getUser().getUserId(),
                participant.getUser().getName(),
                participant.getUser().getEmail(),
                participant.getRole(),
                participant.getCreatedAt()
        );
    }
}
