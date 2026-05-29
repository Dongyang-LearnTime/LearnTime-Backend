package learntime.backend.domain.study.converter;

import learntime.backend.domain.study.dto.response.StudyArchiveResponseDTO;
import learntime.backend.domain.study.model.Study;
import learntime.backend.domain.study_member.model.StudyMember;
import learntime.backend.global.error.code.ErrorCode;
import learntime.backend.global.error.exception.BusinessException;

public class StudyArchiveConverter {

    public StudyArchiveConverter() {
        throw new BusinessException(ErrorCode.UTILITY_CLASS_INSTANTIATION);
    }

    public static StudyArchiveResponseDTO toStudyArchiveResponseDTO(StudyMember member) {
        Study study = member.getStudy();
        return StudyArchiveResponseDTO.builder()
                .studyId(study.getStudyId())
                .studyTitle(study.getStudyTitle())
                .bookTitle(study.getBookTitle())
                .startDate(study.getStartDate())
                .endDate(study.getEndDate())
                .myRole(member.getStudyMemberRole())
                .myStatus(member.getStatus())
                .joinedAt(member.getJoinedAt())
                .build();
    }
}
