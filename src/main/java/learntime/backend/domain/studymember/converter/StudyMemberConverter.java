package learntime.backend.domain.studymember.converter;

import learntime.backend.domain.studymember.dto.response.StudyInvitationResponseDTO;
import learntime.backend.domain.studymember.dto.response.StudyMemberResponseDTO;
import learntime.backend.domain.studymember.model.StudyInvitation;
import learntime.backend.domain.studymember.model.StudyMember;
import learntime.backend.global.error.code.ErrorCode;
import learntime.backend.global.error.exception.BusinessException;

public class StudyMemberConverter {

    public StudyMemberConverter() {
        throw new BusinessException(ErrorCode.UTILITY_CLASS_INSTANTIATION);
    }

    // 초대 받은 목록
    public static StudyInvitationResponseDTO toInvitationReceivedResponse(StudyInvitation studyInvitation) {
        return StudyInvitationResponseDTO.builder()
                .studyInvitationId(studyInvitation.getStudyInvitationId())
                .studyId(studyInvitation.getStudy().getStudyId())
                .studyTitle(studyInvitation.getStudy().getStudyTitle())
                .userId(studyInvitation.getInviterUser().getUserId())
                .userName(studyInvitation.getInviterUser().getName())
                .requestedAt(studyInvitation.getRequestedAt())
                .build();
    }

    // 초대한 목록
    public static StudyInvitationResponseDTO toInvitationSentResponse(StudyInvitation studyInvitation) {
        return StudyInvitationResponseDTO.builder()
                .studyInvitationId(studyInvitation.getStudyInvitationId())
                .studyId(studyInvitation.getStudy().getStudyId())
                .studyTitle(studyInvitation.getStudy().getStudyTitle())
                .userId(studyInvitation.getInvitedUser().getUserId())
                .userName(studyInvitation.getInvitedUser().getName())
                .requestedAt(studyInvitation.getRequestedAt())
                .build();
    }

    public static StudyMemberResponseDTO toStudyMemberResponse(StudyMember studyMember) {
        return StudyMemberResponseDTO.builder()
                .studyMemberId(studyMember.getStudyMemberId())
                .studyMemberRole(studyMember.getStudyMemberRole())
                .joinedAt(studyMember.getJoinedAt())
                .userId(studyMember.getUser().getUserId())
                .userName(studyMember.getUser().getName())
                .build();
    }

}
