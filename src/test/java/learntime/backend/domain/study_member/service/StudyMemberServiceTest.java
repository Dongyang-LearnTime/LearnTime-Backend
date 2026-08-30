package learntime.backend.domain.study_member.service;

import learntime.backend.domain.relationship.repository.UserBlockRepository;
import learntime.backend.domain.study.error.code.StudyErrorCode;
import learntime.backend.domain.study.error.exception.StudyException;
import learntime.backend.domain.study.model.Study;
import learntime.backend.domain.study.repository.StudyRepository;
import learntime.backend.domain.study_member.enums.StudyMemberRole;
import learntime.backend.domain.study_member.enums.StudyMemberStatus;
import learntime.backend.domain.study_member.model.StudyMember;
import learntime.backend.domain.study_member.repository.StudyMemberRepository;
import learntime.backend.domain.user.model.User;
import learntime.backend.domain.user.repository.UserRepository;
import learntime.backend.global.utils.UserBlockUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StudyMemberServiceTest {

    @Mock
    private StudyMemberRepository studyMemberRepository;

    @Mock
    private StudyRepository studyRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserBlockRepository userBlockRepository;

    @Mock
    private UserBlockUtil userBlockUtil;

    @InjectMocks
    private StudyMemberService studyMemberService;

    @Test
    @DisplayName("공개 스터디 참여 성공 - 정상 참여")
    void joinPublicStudy_Success() {
        // given
        Long studyId = 1L;
        Long userId = 2L;
        Long ownerId = 1L;

        User user = mock(User.class);
        given(userRepository.findById(userId)).willReturn(Optional.of(user));

        Study study = mock(Study.class);
        given(study.getIsPublic()).willReturn(true);
        given(studyRepository.findByIdWithPessimisticLock(studyId)).willReturn(Optional.of(study));

        given(studyMemberRepository.countByStudyAndStatusIn(study, List.of(StudyMemberStatus.ACTIVE, StudyMemberStatus.COMPLETED)))
                .willReturn(1L);
        given(studyMemberRepository.existsByStudy_StudyIdAndUser_UserId(studyId, userId)).willReturn(false);

        User ownerUser = mock(User.class);
        given(ownerUser.getUserId()).willReturn(ownerId);
        StudyMember ownerMember = mock(StudyMember.class);
        given(ownerMember.getUser()).willReturn(ownerUser);
        given(studyMemberRepository.findByStudy_StudyIdAndStudyMemberRoleAndStatus(studyId, StudyMemberRole.OWNER, StudyMemberStatus.ACTIVE))
                .willReturn(Optional.of(ownerMember));

        StudyMember savedMember = mock(StudyMember.class);
        given(savedMember.getStudyMemberId()).willReturn(10L);
        given(studyMemberRepository.save(any(StudyMember.class))).willReturn(savedMember);

        // when
        Long result = studyMemberService.joinPublicStudy(studyId, userId);

        // then
        assertThat(result).isEqualTo(10L);
        verify(userBlockUtil).validateNotBlockedByUser(ownerId, userId);
        verify(studyMemberRepository).save(any(StudyMember.class));
    }

    @Test
    @DisplayName("공개 스터디 참여 실패 - 비공개 스터디")
    void joinPublicStudy_Fail_NotPublic() {
        // given
        Long studyId = 1L;
        Long userId = 2L;

        User user = mock(User.class);
        given(userRepository.findById(userId)).willReturn(Optional.of(user));

        Study study = mock(Study.class);
        given(study.getIsPublic()).willReturn(false);
        given(studyRepository.findByIdWithPessimisticLock(studyId)).willReturn(Optional.of(study));

        // when & then
        assertThatThrownBy(() -> studyMemberService.joinPublicStudy(studyId, userId))
                .isInstanceOf(StudyException.class)
                .hasMessageContaining(StudyErrorCode.STUDY_NOT_PUBLIC.getMessage());
    }

    @Test
    @DisplayName("공개 스터디 참여 실패 - 정원(4명) 초과")
    void joinPublicStudy_Fail_LimitExceeded() {
        // given
        Long studyId = 1L;
        Long userId = 2L;

        User user = mock(User.class);
        given(userRepository.findById(userId)).willReturn(Optional.of(user));

        Study study = mock(Study.class);
        given(study.getIsPublic()).willReturn(true);
        given(studyRepository.findByIdWithPessimisticLock(studyId)).willReturn(Optional.of(study));

        given(studyMemberRepository.countByStudyAndStatusIn(study, List.of(StudyMemberStatus.ACTIVE, StudyMemberStatus.COMPLETED)))
                .willReturn(4L);

        // when & then
        assertThatThrownBy(() -> studyMemberService.joinPublicStudy(studyId, userId))
                .isInstanceOf(StudyException.class)
                .hasMessageContaining(StudyErrorCode.STUDY_MEMBER_LIMIT_EXCEEDED.getMessage());
    }

    @Test
    @DisplayName("공개 스터디 참여 실패 - 이미 멤버임")
    void joinPublicStudy_Fail_AlreadyMember() {
        // given
        Long studyId = 1L;
        Long userId = 2L;

        User user = mock(User.class);
        given(userRepository.findById(userId)).willReturn(Optional.of(user));

        Study study = mock(Study.class);
        given(study.getIsPublic()).willReturn(true);
        given(studyRepository.findByIdWithPessimisticLock(studyId)).willReturn(Optional.of(study));

        given(studyMemberRepository.countByStudyAndStatusIn(study, List.of(StudyMemberStatus.ACTIVE, StudyMemberStatus.COMPLETED)))
                .willReturn(1L);
        given(studyMemberRepository.existsByStudy_StudyIdAndUser_UserId(studyId, userId)).willReturn(true);

        // when & then
        assertThatThrownBy(() -> studyMemberService.joinPublicStudy(studyId, userId))
                .isInstanceOf(StudyException.class)
                .hasMessageContaining(StudyErrorCode.ALREADY_STUDY_MEMBER.getMessage());
    }
}
