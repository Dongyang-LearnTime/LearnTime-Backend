package learntime.backend.domain.studymember.event;

public record StudyInvitationSentEvent(
        Long studyInvitationId, // 초대 요청 ID
        Long studyId,
        String studyTitle,
        String inviterName, // 초대자 닉네임
        Long invitedUserId // 초대 받은 사람 ID (User)
) {
}
