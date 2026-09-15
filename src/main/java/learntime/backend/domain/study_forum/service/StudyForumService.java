package learntime.backend.domain.study_forum.service;

import learntime.backend.domain.study.model.Study;
import learntime.backend.domain.study.repository.StudyRepository;
import learntime.backend.domain.study.error.code.StudyErrorCode;
import learntime.backend.domain.study.error.exception.StudyException;
import learntime.backend.domain.study_forum.converter.StudyForumMessageConverter;
import learntime.backend.domain.study_forum.dto.request.StudyForumMessageRequestDTO;
import learntime.backend.domain.study_forum.dto.response.StudyForumMessageResponseDTO;
import learntime.backend.domain.study_forum.error.code.StudyForumErrorCode;
import learntime.backend.domain.study_forum.error.exception.StudyForumException;
import learntime.backend.domain.study_forum.model.StudyForumMessage;
import learntime.backend.domain.study_forum.repository.StudyForumMessageRepository;
import learntime.backend.domain.study_member.enums.StudyMemberStatus;
import learntime.backend.domain.study_member.model.StudyMember;
import learntime.backend.domain.study_member.repository.StudyMemberRepository;
import learntime.backend.domain.user.repository.UserRepository;
import learntime.backend.global.dto.CursorResponse;
import learntime.backend.global.error.code.AuthErrorCode;
import learntime.backend.global.error.exception.AuthException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StudyForumService {
    private final StudyForumMessageRepository messageRepository;
    private final StudyMemberRepository memberRepository;
    private final StudyRepository studyRepository;
    private final UserRepository userRepository;

    public CursorResponse<StudyForumMessageResponseDTO> getMessages(Long studyId, Long userId, Long beforeId, Integer size) {
        verifyMember(studyId, userId);
        if (size == null || size < 1 || size > 100 || (beforeId != null && beforeId < 1)) {
            throw new StudyForumException(StudyForumErrorCode.INVALID_REQUEST);
        }
        List<StudyForumMessage> rows = messageRepository.findMessages(studyId, userId, beforeId, PageRequest.of(0, size + 1));
        boolean hasNext = rows.size() > size;
        List<StudyForumMessage> page = hasNext ? rows.subList(0, size) : rows;
        return CursorResponse.of(page.stream().map(StudyForumMessageConverter::toResponse).toList(),
                hasNext ? page.get(page.size() - 1).getMessageId() : null, hasNext);
    }

    @Transactional
    public StudyForumMessageResponseDTO create(Long studyId, Long userId, StudyForumMessageRequestDTO request) {
        if (request == null || request.content() == null || request.content().isBlank() || request.content().length() > 1000) {
            throw new StudyForumException(StudyForumErrorCode.INVALID_REQUEST);
        }
        // Serialize with account withdrawal and study deletion before checking membership.
        userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new AuthException(AuthErrorCode.USER_NOT_FOUND));
        Study study = studyRepository.findByIdWithPessimisticLock(studyId)
                .orElseThrow(() -> new StudyException(StudyErrorCode.STUDY_NOT_FOUND));
        StudyMember member = memberRepository.findForForumWrite(studyId, userId)
                .filter(value -> value.getStatus() == StudyMemberStatus.ACTIVE || value.getStatus() == StudyMemberStatus.COMPLETED)
                .orElseThrow(() -> new StudyForumException(StudyForumErrorCode.ACCESS_DENIED));
        if (member.getStatus() == StudyMemberStatus.COMPLETED) {
            throw new StudyForumException(StudyForumErrorCode.READ_ONLY);
        }
        return StudyForumMessageConverter.toResponse(
                messageRepository.save(StudyForumMessageConverter.toEntity(study, member, request)));
    }

    @Transactional
    public void delete(Long studyId, Long messageId, Long userId) {
        verifyMember(studyId, userId);
        StudyForumMessage message = messageRepository.findByMessageIdAndStudy_StudyId(messageId, studyId)
                .orElseThrow(() -> new StudyForumException(StudyForumErrorCode.MESSAGE_NOT_FOUND));
        if (message.getAuthor() == null || message.getAuthor().getUser() == null
                || !message.getAuthor().getUser().getUserId().equals(userId)) {
            throw new StudyForumException(StudyForumErrorCode.NOT_AUTHOR);
        }
        messageRepository.delete(message);
    }

    private StudyMember verifyMember(Long studyId, Long userId) {
        return memberRepository.findByStudy_StudyIdAndUser_UserIdAndStatusIn(
                studyId, userId, List.of(StudyMemberStatus.ACTIVE, StudyMemberStatus.COMPLETED))
                .orElseThrow(() -> new StudyForumException(StudyForumErrorCode.ACCESS_DENIED));
    }
}

