package learntime.backend.domain.study.service.core;

import learntime.backend.domain.study.error.code.StudyErrorCode;
import learntime.backend.domain.study.error.exception.StudyException;
import learntime.backend.domain.study.model.Study;
import learntime.backend.domain.study.repository.StudyRepository;
import learntime.backend.domain.study_member.enums.StudyMemberRole;
import learntime.backend.domain.study_member.enums.StudyMemberStatus;
import learntime.backend.domain.study_member.model.StudyMember;
import learntime.backend.domain.study_member.repository.StudyMemberRepository;
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
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StudyManagementServiceTest {

    @Mock
    private StudyRepository studyRepository;

    @Mock
    private StudyMemberRepository studyMemberRepository;

    @InjectMocks
    private StudyManagementService studyManagementService;

    @Test
    @DisplayName("스터디 삭제 실패 - 존재하지 않는 스터디")
    void deleteStudyBulk_StudyNotFound() {
        // given
        Long studyId = 1L;
        Long userId = 1L;
        given(studyRepository.existsById(studyId)).willReturn(false);

        // when & then
        assertThatThrownBy(() -> studyManagementService.deleteStudyBulk(studyId, userId))
                .isInstanceOf(StudyException.class)
                .hasMessageContaining(StudyErrorCode.STUDY_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("스터디 삭제 실패 - 스터디 멤버가 아님")
    void deleteStudyBulk_StudyMemberNotFound() {
        // given
        Long studyId = 1L;
        Long userId = 1L;
        given(studyRepository.existsById(studyId)).willReturn(true);
        given(studyMemberRepository.findByStudy_StudyIdAndUser_UserIdAndStatusIn(studyId, userId, List.of(StudyMemberStatus.ACTIVE, StudyMemberStatus.COMPLETED)))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> studyManagementService.deleteStudyBulk(studyId, userId))
                .isInstanceOf(StudyException.class)
                .hasMessageContaining(StudyErrorCode.STUDY_MEMBER_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("스터디 삭제 실패 - 방장이 아님")
    void deleteStudyBulk_NotOwner() {
        // given
        Long studyId = 1L;
        Long userId = 1L;
        given(studyRepository.existsById(studyId)).willReturn(true);
        
        StudyMember studyMember = mock(StudyMember.class);
        given(studyMember.isActive()).willReturn(true);
        given(studyMember.getStudyMemberRole()).willReturn(StudyMemberRole.MEMBER); // MEMBER 권한
        
        given(studyMemberRepository.findByStudy_StudyIdAndUser_UserIdAndStatusIn(studyId, userId, List.of(StudyMemberStatus.ACTIVE, StudyMemberStatus.COMPLETED)))
                .willReturn(Optional.of(studyMember));

        // when & then
        assertThatThrownBy(() -> studyManagementService.deleteStudyBulk(studyId, userId))
                .isInstanceOf(StudyException.class)
                .hasMessageContaining(StudyErrorCode.STUDY_UNAUTHORIZED_ACCESS.getMessage());
    }

    @Test
    @DisplayName("스터디 삭제 성공 - 연관된 데이터 벌크 삭제 확인")
    void deleteStudyBulk_Success() {
        // given
        Long studyId = 1L;
        Long userId = 1L;
        given(studyRepository.existsById(studyId)).willReturn(true);
        
        StudyMember studyMember = mock(StudyMember.class);
        given(studyMember.isActive()).willReturn(true);
        given(studyMember.getStudyMemberRole()).willReturn(StudyMemberRole.OWNER); // 방장 권한
        
        given(studyMemberRepository.findByStudy_StudyIdAndUser_UserIdAndStatusIn(studyId, userId, List.of(StudyMemberStatus.ACTIVE, StudyMemberStatus.COMPLETED)))
                .willReturn(Optional.of(studyMember));

        // when
        studyManagementService.deleteStudyBulk(studyId, userId);

        // then
        // 1계층 삭제 확인
        verify(studyRepository).deleteStudyMemberContentsByStudyId(studyId);
        verify(studyRepository).deleteStudyStatusesByStudyId(studyId);
        verify(studyRepository).deleteStudyFeedbacksByStudyId(studyId);
        verify(studyRepository).deleteQuizHistoriesByStudyId(studyId);
        verify(studyRepository).deleteQuizQuestionsByStudyId(studyId);

        // 2계층 삭제 확인
        verify(studyRepository).deleteStudyQuizzesByStudyId(studyId);

        // 3계층 삭제 확인
        verify(studyRepository).deleteStudyDailyPlansByStudyId(studyId);
        verify(studyRepository).deleteStudyRestDatesByStudyId(studyId);
        verify(studyRepository).deleteStudyRestDaysByStudyId(studyId);
        verify(studyRepository).deleteStudyInvitationsByStudyId(studyId);

        // 4계층 삭제 확인
        verify(studyRepository).deleteStudyMembersByStudyId(studyId);
        verify(studyRepository).deleteStudyById(studyId);
    }

    @Test
    @DisplayName("스터디 공개 여부 변경 성공 - 방장")
    void updateVisibility_Success() {
        // given
        Long studyId = 1L;
        Long userId = 1L;
        Study study = mock(Study.class);
        given(studyRepository.findById(studyId)).willReturn(Optional.of(study));

        StudyMember studyMember = mock(StudyMember.class);
        given(studyMember.isActive()).willReturn(true);
        given(studyMember.getStudyMemberRole()).willReturn(StudyMemberRole.OWNER);

        given(studyMemberRepository.findByStudy_StudyIdAndUser_UserIdAndStatusIn(studyId, userId, List.of(StudyMemberStatus.ACTIVE, StudyMemberStatus.COMPLETED)))
                .willReturn(Optional.of(studyMember));

        // when
        studyManagementService.updateVisibility(studyId, true, userId);

        // then
        verify(study).updateVisibility(true);
    }

    @Test
    @DisplayName("스터디 공개 여부 변경 실패 - 방장이 아님")
    void updateVisibility_NotOwner() {
        // given
        Long studyId = 1L;
        Long userId = 1L;
        Study study = mock(Study.class);
        given(studyRepository.findById(studyId)).willReturn(Optional.of(study));

        StudyMember studyMember = mock(StudyMember.class);
        given(studyMember.isActive()).willReturn(true);
        given(studyMember.getStudyMemberRole()).willReturn(StudyMemberRole.MEMBER);

        given(studyMemberRepository.findByStudy_StudyIdAndUser_UserIdAndStatusIn(studyId, userId, List.of(StudyMemberStatus.ACTIVE, StudyMemberStatus.COMPLETED)))
                .willReturn(Optional.of(studyMember));

        // when & then
        assertThatThrownBy(() -> studyManagementService.updateVisibility(studyId, true, userId))
                .isInstanceOf(StudyException.class)
                .hasMessageContaining(StudyErrorCode.STUDY_UNAUTHORIZED_ACCESS.getMessage());
    }

    private StudyMember mockOwnerMember() {
        StudyMember studyMember = mock(StudyMember.class);
        given(studyMember.isActive()).willReturn(true);
        given(studyMember.getStudyMemberRole()).willReturn(StudyMemberRole.OWNER);
        return studyMember;
    }
}
