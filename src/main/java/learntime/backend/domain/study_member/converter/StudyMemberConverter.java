package learntime.backend.domain.study_member.converter;

import learntime.backend.domain.study.model.Study;
import learntime.backend.domain.study_member.dto.response.StudyInvitationResponseDTO;
import learntime.backend.domain.study_member.dto.response.StudyMemberFriendResponseDTO;
import learntime.backend.domain.study_member.dto.response.StudyMemberResponseDTO;
import learntime.backend.domain.study_member.model.StudyInvitation;
import learntime.backend.domain.study_member.model.StudyMember;
import learntime.backend.domain.relationship.model.Friend;
import learntime.backend.domain.user.model.User;
import learntime.backend.global.error.code.ErrorCode;
import learntime.backend.global.error.exception.BusinessException;
import java.util.Set;

public class StudyMemberConverter {

    public StudyMemberConverter() {
        throw new BusinessException(ErrorCode.UTILITY_CLASS_INSTANTIATION);
    }

    public static StudyInvitation toStudyInvitation(Study study, User invitedUser, User inviterUser) {
        return StudyInvitation.builder()
                .study(study)
                .invitedUser(invitedUser)
                .inviterUser(inviterUser)
                .build();
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
        String profileImageUrl = null;
        if (studyMember.getUser() != null && studyMember.getUser().getProfile() != null) {
            profileImageUrl = studyMember.getUser().getProfile().getProfileImageUrl();
        }
        
        return StudyMemberResponseDTO.builder()
                .studyMemberId(studyMember.getStudyMemberId())
                .studyMemberRole(studyMember.getStudyMemberRole())
                .joinedAt(studyMember.getJoinedAt())
                .status(studyMember.getStatus())
                .userId(studyMember.getUser().getUserId())
                .userName(studyMember.getUser().getName())
                .profileImageUrl(profileImageUrl)
                .build();
    }

    public static StudyMemberFriendResponseDTO toStudyMemberFriendResponseDTO(
            Friend friend,
            Long currentUserId,
            Set<Long> pendingInvitedUserIds
    ) {
        User friendUser = friend.getUser().getUserId().equals(currentUserId)
                ? friend.getFriendUser()
                : friend.getUser();

        boolean isInvited = pendingInvitedUserIds.contains(friendUser.getUserId());

        return new StudyMemberFriendResponseDTO(
                friend.getFriendId(),
                friendUser.getUserId(),
                friendUser.getName(),
                friendUser.getEmail(),
                friend.getCreatedAt(),
                isInvited
        );
    }

}
