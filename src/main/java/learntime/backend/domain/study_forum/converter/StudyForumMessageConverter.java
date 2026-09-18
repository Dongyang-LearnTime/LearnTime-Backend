package learntime.backend.domain.study_forum.converter;

import learntime.backend.domain.study.model.Study;
import learntime.backend.domain.study_forum.dto.request.StudyForumMessageRequestDTO;
import learntime.backend.domain.study_forum.dto.response.StudyForumMessageResponseDTO;
import learntime.backend.domain.study_forum.model.StudyForumMessage;
import learntime.backend.domain.user.model.User;
import learntime.backend.domain.study_member.model.StudyMember;

public final class StudyForumMessageConverter {
    private StudyForumMessageConverter() {}

    public static StudyForumMessage toEntity(Study study, StudyMember author, StudyForumMessageRequestDTO request) {
        return StudyForumMessage.builder().study(study).author(author).content(request.content().strip()).build();
    }

    public static StudyForumMessageResponseDTO toResponse(StudyForumMessage message) {
        StudyMember member = message.getAuthor();
        User author = member == null ? null : member.getUser();
        return new StudyForumMessageResponseDTO(message.getMessageId(),
                member == null ? null : member.getStudyMemberId(),
                author == null ? null : author.getUserId(),
                author == null ? "탈퇴한 사용자" : author.getName(),
                message.getContent(), message.getCreatedAt());
    }
}

