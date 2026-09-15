package learntime.backend.domain.study_forum.service;

import learntime.backend.domain.study.model.Study;
import learntime.backend.domain.study.repository.StudyRepository;
import learntime.backend.domain.study_forum.dto.request.StudyForumMessageRequestDTO;
import learntime.backend.domain.study_forum.error.code.StudyForumErrorCode;
import learntime.backend.domain.study_forum.error.exception.StudyForumException;
import learntime.backend.domain.study_forum.repository.StudyForumMessageRepository;
import learntime.backend.domain.study_member.enums.StudyMemberRole;
import learntime.backend.domain.study_member.enums.StudyMemberStatus;
import learntime.backend.domain.study_member.model.StudyMember;
import learntime.backend.domain.study_member.repository.StudyMemberRepository;
import learntime.backend.domain.user.model.User;
import learntime.backend.domain.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.List;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StudyForumServiceTest {
    @Mock StudyForumMessageRepository messageRepository;
    @Mock StudyMemberRepository memberRepository;
    @Mock StudyRepository studyRepository;
    @Mock UserRepository userRepository;
    @InjectMocks StudyForumService service;

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\n\t"})
    void rejectBlankContent(String content) {
        assertThatThrownBy(() -> service.create(1L, 2L, new StudyForumMessageRequestDTO(content)))
                .isInstanceOf(StudyForumException.class)
                .hasMessage(StudyForumErrorCode.INVALID_REQUEST.getMessage());
        verifyNoInteractions(messageRepository);
    }

    @Test
    void rejectOversizedContent() {
        assertThatThrownBy(() -> service.create(1L, 2L, new StudyForumMessageRequestDTO("a".repeat(1001))))
                .isInstanceOf(StudyForumException.class);
        verifyNoInteractions(messageRepository);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, 101})
    void rejectPageSizeOutsideLimit(int size) {
        allowRead();
        assertThatThrownBy(() -> service.getMessages(1L, 2L, null, size))
                .hasMessage(StudyForumErrorCode.INVALID_REQUEST.getMessage());
        verifyNoInteractions(messageRepository);
    }

    @Test
    void rejectInvalidCursor() {
        allowRead();
        assertThatThrownBy(() -> service.getMessages(1L, 2L, 0L, 30))
                .hasMessage(StudyForumErrorCode.INVALID_REQUEST.getMessage());
        verifyNoInteractions(messageRepository);
    }

    @Test
    void messageIdFromAnotherStudyIsNotDeleted() {
        allowRead();
        when(messageRepository.findByMessageIdAndStudy_StudyId(9L, 1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.delete(1L, 9L, 2L))
                .hasMessage(StudyForumErrorCode.MESSAGE_NOT_FOUND.getMessage());
        verify(messageRepository, never()).delete(any());
    }

    @Test
    void withdrawnMemberCannotWriteEvenIfUserAndStudyStillExist() {
        when(userRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(mock(User.class)));
        when(studyRepository.findByIdWithPessimisticLock(1L)).thenReturn(Optional.of(mock(Study.class)));
        when(memberRepository.findForForumWrite(1L, 2L)).thenReturn(Optional.of(
                StudyMember.builder().status(StudyMemberStatus.WITHDRAWN).studyMemberRole(StudyMemberRole.MEMBER).build()));
        assertThatThrownBy(() -> service.create(1L, 2L, new StudyForumMessageRequestDTO("hello")))
                .hasMessage(StudyForumErrorCode.ACCESS_DENIED.getMessage());
        verifyNoInteractions(messageRepository);
    }

    private void allowRead() {
        when(memberRepository.findByStudy_StudyIdAndUser_UserIdAndStatusIn(
                1L, 2L, List.of(StudyMemberStatus.ACTIVE, StudyMemberStatus.COMPLETED)))
                .thenReturn(Optional.of(StudyMember.builder().status(StudyMemberStatus.ACTIVE).build()));
    }
}

