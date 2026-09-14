package learntime.backend.domain.study_member.converter;

import learntime.backend.domain.study.model.Study;
import learntime.backend.domain.study_member.dto.response.StudyJoinRequestResponseDTO;
import learntime.backend.domain.study_member.model.StudyJoinRequest;
import learntime.backend.domain.user.model.User;

public class StudyJoinRequestConverter {

    public static StudyJoinRequest toEntity(Study study, User requesterUser) {
        return StudyJoinRequest.builder()
                .study(study)
                .requesterUser(requesterUser)
                .build();
    }

    public static StudyJoinRequestResponseDTO toResponseDTO(StudyJoinRequest joinRequest) {
        return new StudyJoinRequestResponseDTO(
                joinRequest.getStudyJoinRequestId(),
                joinRequest.getStudy().getStudyId(),
                joinRequest.getStudy().getStudyTitle(),
                joinRequest.getRequesterUser().getUserId(),
                joinRequest.getRequesterUser().getName(),
                joinRequest.getStatus(),
                joinRequest.getCreatedAt()
        );
    }
}
