package learntime.backend.domain.study.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import learntime.backend.domain.study.enums.StudyParticipantRole;
import learntime.backend.domain.study.model.Study;
import learntime.backend.domain.study.model.StudyParticipant;

import java.time.LocalDate;

@Schema(description = "공유 스터디 목록 조회 응답 DTO")
public record SharedStudyResponseDTO(
        Long studyId,             // 스터디 식별자
        String studyTitle,        // 진도 제목
        String bookTitle,         // 책 제목
        LocalDate startDate,      // 스터디 시작일
        LocalDate endDate,        // 스터디 종료일
        StudyParticipantRole role,// 참여자 역할 (OWNER, MEMBER)
        Long ownerId,             // 방장(소유자) 식별자
        String ownerName          // 방장(소유자) 이름
) {
    public static SharedStudyResponseDTO from(StudyParticipant participant) {
        Study study = participant.getStudy();
        return new SharedStudyResponseDTO(
                study.getStudyId(),
                study.getStudyTitle(),
                study.getBookTitle(),
                study.getStartDate(),
                study.getEndDate(),
                participant.getRole(),
                study.getUser().getUserId(),
                study.getUser().getName()
        );
    }
}
