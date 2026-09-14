package learntime.backend.domain.study.service.core;

import learntime.backend.domain.notes.dto.response.StudyNotesResponseDTO;
import learntime.backend.domain.notes.model.StudyNotes;
import learntime.backend.domain.notes.repository.StudyNotesRepository;
import learntime.backend.domain.notes.service.StudyNotesService;
import learntime.backend.domain.study.error.code.StudyErrorCode;
import learntime.backend.domain.study.error.exception.StudyException;
import learntime.backend.domain.study_member.enums.StudyMemberStatus;
import learntime.backend.domain.study_member.model.StudyMember;
import learntime.backend.domain.study_member.repository.StudyMemberRepository;
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
@DisplayName("StudyNotesService 단위 테스트")
class StudyNotesServiceTest {

    @Mock private StudyNotesRepository studyNotesRepository;
    @Mock private StudyMemberRepository studyMemberRepository;

    @InjectMocks
    private StudyNotesService studyNotesService;

    // ──────────────────────────────────────────────────────────
    // getNotesList — WITHDRAWN 허용 검증
    // ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("getNotesList - ACTIVE 멤버는 필기 목록을 조회할 수 있다")
    void getNotesList_ActiveMember_Success() {
        // given
        Long studyId = 1L, userId = 10L;
        Pageable pageable = PageRequest.of(0, 10);
        StudyMember member = mockMemberWithStatus(StudyMemberStatus.ACTIVE, 100L);

        given(studyMemberRepository.findByStudy_StudyIdAndUser_UserIdAndStatusIn(
                studyId, userId,
                List.of(StudyMemberStatus.ACTIVE, StudyMemberStatus.WITHDRAWN, StudyMemberStatus.COMPLETED)))
                .willReturn(Optional.of(member));
        given(studyMemberRepository.existsByStudy_StudyIdAndUser_UserIdAndStatusIn(
                studyId, userId,
                List.of(StudyMemberStatus.ACTIVE, StudyMemberStatus.WITHDRAWN, StudyMemberStatus.COMPLETED)))
                .willReturn(true);
        given(studyNotesRepository.findByStudyMember(member, pageable))
                .willReturn(new PageImpl<>(List.of()));

        // when
        PageResponse<StudyNotesResponseDTO> result =
                studyNotesService.getNotesList(studyId, pageable, userId);

        // then
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("getNotesList - WITHDRAWN 멤버도 자신의 필기 목록을 조회할 수 있다")
    void getNotesList_WithdrawnMember_Success() {
        // given
        Long studyId = 1L, userId = 20L;
        Pageable pageable = PageRequest.of(0, 10);
        StudyMember member = mockMemberWithStatus(StudyMemberStatus.WITHDRAWN, 200L);

        given(studyMemberRepository.findByStudy_StudyIdAndUser_UserIdAndStatusIn(
                studyId, userId,
                List.of(StudyMemberStatus.ACTIVE, StudyMemberStatus.WITHDRAWN, StudyMemberStatus.COMPLETED)))
                .willReturn(Optional.of(member));
        given(studyMemberRepository.existsByStudy_StudyIdAndUser_UserIdAndStatusIn(
                studyId, userId,
                List.of(StudyMemberStatus.ACTIVE, StudyMemberStatus.WITHDRAWN, StudyMemberStatus.COMPLETED)))
                .willReturn(true);
        given(studyNotesRepository.findByStudyMember(member, pageable))
                .willReturn(new PageImpl<>(List.of()));

        // when & then
        assertThatCode(() -> studyNotesService.getNotesList(studyId, pageable, userId))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("getNotesList - 스터디 멤버가 아닌 사용자는 조회할 수 없다")
    void getNotesList_NotMember_ThrowsException() {
        // given
        Long studyId = 1L, userId = 99L;
        Pageable pageable = PageRequest.of(0, 10);

        given(studyMemberRepository.findByStudy_StudyIdAndUser_UserIdAndStatusIn(
                studyId, userId,
                List.of(StudyMemberStatus.ACTIVE, StudyMemberStatus.WITHDRAWN, StudyMemberStatus.COMPLETED)))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> studyNotesService.getNotesList(studyId, pageable, userId))
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
