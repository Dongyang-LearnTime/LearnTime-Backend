package learntime.backend.domain.study.service.core;

import learntime.backend.domain.study_feedback.service.core.StudyFeedbackResponseDTO;
import learntime.backend.domain.study.error.code.StudyErrorCode;
import learntime.backend.domain.study.error.exception.StudyException;
import learntime.backend.domain.study_feedback.repository.StudyFeedbackRepository;
import learntime.backend.domain.study_feedback.service.ai.GeminiFeedbackService;
import learntime.backend.domain.study_feedback.service.core.StudyFeedbackService;
import learntime.backend.domain.study_member.enums.StudyMemberStatus;
import learntime.backend.domain.study_member.model.StudyMember;
import learntime.backend.domain.study_member.repository.StudyMemberRepository;
import learntime.backend.domain.study_progress.service.StudyQueryService;
import learntime.backend.global.dto.PageResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("StudyFeedbackService 단위 테스트")
class StudyFeedbackServiceTest {

    @Mock private StudyFeedbackRepository studyFeedbackRepository;
    @Mock private StudyMemberRepository studyMemberRepository;
    @Mock private StudyQueryService studyQueryService;
    @Mock private GeminiFeedbackService geminiFeedbackService;

    @InjectMocks
    private StudyFeedbackService studyFeedbackService;

    // ──────────────────────────────────────────────────────────
    // getMemberFeedbacks — WITHDRAWN 허용 검증
    // ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("getMemberFeedbacks - ACTIVE 멤버는 피드백을 조회할 수 있다")
    void getMemberFeedbacks_ActiveMember_Success() {
        // given
        Long studyId = 1L, userId = 10L;
        StudyMember member = mockMemberWithStatus(StudyMemberStatus.ACTIVE, 100L);
        Pageable pageable = PageRequest.of(0, 10);

        given(studyMemberRepository.findByStudy_StudyIdAndUser_UserIdAndStatusIn(
                studyId, userId,
                List.of(StudyMemberStatus.ACTIVE, StudyMemberStatus.WITHDRAWN, StudyMemberStatus.COMPLETED)))
                .willReturn(Optional.of(member));
        given(studyFeedbackRepository.findAllByStudyMember_StudyMemberId(100L, pageable))
                .willReturn(new PageImpl<>(List.of()));

        // when
        PageResponse<StudyFeedbackResponseDTO> result =
                studyFeedbackService.getMemberFeedbacks(studyId, pageable, userId);

        // then
        assertThat(result).isNotNull();
        verify(studyMemberRepository).findByStudy_StudyIdAndUser_UserIdAndStatusIn(
                studyId, userId,
                List.of(StudyMemberStatus.ACTIVE, StudyMemberStatus.WITHDRAWN, StudyMemberStatus.COMPLETED));
    }

    @Test
    @DisplayName("getMemberFeedbacks - WITHDRAWN 멤버도 자신의 과거 피드백을 조회할 수 있다")
    void getMemberFeedbacks_WithdrawnMember_Success() {
        // given
        Long studyId = 1L, userId = 20L;
        StudyMember member = mockMemberWithStatus(StudyMemberStatus.WITHDRAWN, 200L);
        Pageable pageable = PageRequest.of(0, 10);

        given(studyMemberRepository.findByStudy_StudyIdAndUser_UserIdAndStatusIn(
                studyId, userId,
                List.of(StudyMemberStatus.ACTIVE, StudyMemberStatus.WITHDRAWN, StudyMemberStatus.COMPLETED)))
                .willReturn(Optional.of(member));
        given(studyFeedbackRepository.findAllByStudyMember_StudyMemberId(200L, pageable))
                .willReturn(new PageImpl<>(List.of()));

        // when & then (예외 없이 성공해야 함)
        assertThatCode(() -> studyFeedbackService.getMemberFeedbacks(studyId, pageable, userId))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("getMemberFeedbacks - 해당 스터디 멤버가 아니면 예외가 발생한다")
    void getMemberFeedbacks_NotMember_ThrowsException() {
        // given
        Long studyId = 1L, userId = 99L;
        Pageable pageable = PageRequest.of(0, 10);

        given(studyMemberRepository.findByStudy_StudyIdAndUser_UserIdAndStatusIn(
                studyId, userId,
                List.of(StudyMemberStatus.ACTIVE, StudyMemberStatus.WITHDRAWN, StudyMemberStatus.COMPLETED)))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> studyFeedbackService.getMemberFeedbacks(studyId, pageable, userId))
                .isInstanceOf(StudyException.class)
                .hasMessageContaining(StudyErrorCode.STUDY_MEMBER_NOT_FOUND.getMessage());
    }

    // ──────────────────────────────────────────────────────────
    // generateAndSaveFeedback — ACTIVE 전용 검증 (변경 없음 확인)
    // ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("generateAndSaveFeedback - WITHDRAWN 멤버가 피드백 생성 시도 시 예외가 발생한다")
    void generateAndSaveFeedback_WithdrawnMember_ThrowsException() {
        // given
        Long studyId = 1L, userId = 20L;

        given(studyQueryService.getStudyAnalysisData(studyId, userId))
                .willThrow(new StudyException(StudyErrorCode.STUDY_MEMBER_NOT_FOUND));

        // when & then
        assertThatThrownBy(() -> studyFeedbackService.generateAndSaveFeedback(studyId, userId))
                .isInstanceOf(StudyException.class)
                .hasMessageContaining(StudyErrorCode.STUDY_MEMBER_NOT_FOUND.getMessage());
    }

    // ──────────────────────────────────────────────────────────
    // 헬퍼
    // ──────────────────────────────────────────────────────────

    private StudyMember mockMemberWithStatus(StudyMemberStatus status, Long memberId) {
        StudyMember member = mock(StudyMember.class);
        given(member.getStudyMemberId()).willReturn(memberId);
        given(member.getStatus()).willReturn(status);
        given(member.isActive()).willReturn(status == StudyMemberStatus.ACTIVE);
        return member;
    }
}
