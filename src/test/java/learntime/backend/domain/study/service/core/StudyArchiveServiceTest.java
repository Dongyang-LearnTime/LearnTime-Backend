package learntime.backend.domain.study.service.core;

import learntime.backend.domain.study.converter.StudyArchiveConverter;
import learntime.backend.domain.study.dto.response.StudyArchiveResponseDTO;
import learntime.backend.domain.study.dto.response.StudyFeedbackResponseDTO;
import learntime.backend.domain.study.error.code.StudyErrorCode;
import learntime.backend.domain.study.error.exception.StudyException;
import learntime.backend.domain.study.model.Study;
import learntime.backend.domain.study.repository.StudyFeedbackRepository;
import learntime.backend.domain.study_member.enums.StudyMemberRole;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("StudyArchiveService 단위 테스트")
class StudyArchiveServiceTest {

    @Mock private StudyMemberRepository studyMemberRepository;
    @Mock private StudyFeedbackRepository studyFeedbackRepository;

    @InjectMocks
    private StudyArchiveService studyArchiveService;

    // ──────────────────────────────────────────────────────────
    // getMyArchivedStudies
    // ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("getMyArchivedStudies - ACTIVE, WITHDRAWN 멤버십을 모두 반환한다")
    void getMyArchivedStudies_ReturnsBothStatuses() {
        // given
        Long userId = 10L;
        Study study1 = mockStudy(1L, "스터디A");
        Study study2 = mockStudy(2L, "스터디B");

        StudyMember activeMember = mockMember(100L, study1, StudyMemberStatus.ACTIVE, StudyMemberRole.OWNER);
        StudyMember withdrawnMember = mockMember(200L, study2, StudyMemberStatus.WITHDRAWN, StudyMemberRole.MEMBER);

        given(studyMemberRepository.findAllByUserIdAndStatusIn(
                userId,
                List.of(StudyMemberStatus.ACTIVE, StudyMemberStatus.WITHDRAWN)))
                .willReturn(List.of(activeMember, withdrawnMember));

        // when
        List<StudyArchiveResponseDTO> result = studyArchiveService.getMyArchivedStudies(userId);

        // then
        assertThat(result).hasSize(2);
        assertThat(result).extracting(StudyArchiveResponseDTO::myStatus)
                .containsExactlyInAnyOrder(StudyMemberStatus.ACTIVE, StudyMemberStatus.WITHDRAWN);
    }

    @Test
    @DisplayName("getMyArchivedStudies - 참여 이력이 없으면 빈 리스트를 반환한다")
    void getMyArchivedStudies_EmptyResult() {
        // given
        Long userId = 99L;
        given(studyMemberRepository.findAllByUserIdAndStatusIn(
                userId,
                List.of(StudyMemberStatus.ACTIVE, StudyMemberStatus.WITHDRAWN)))
                .willReturn(List.of());

        // when
        List<StudyArchiveResponseDTO> result = studyArchiveService.getMyArchivedStudies(userId);

        // then
        assertThat(result).isEmpty();
    }



    // ──────────────────────────────────────────────────────────
    // 헬퍼
    // ──────────────────────────────────────────────────────────

    private Study mockStudy(Long studyId, String title) {
        Study study = mock(Study.class);
        given(study.getStudyId()).willReturn(studyId);
        given(study.getStudyTitle()).willReturn(title);
        given(study.getBookTitle()).willReturn("교재");
        given(study.getStartDate()).willReturn(LocalDate.now().minusDays(30));
        given(study.getEndDate()).willReturn(LocalDate.now().plusDays(30));
        return study;
    }

    private StudyMember mockMember(Long memberId, Study study, StudyMemberStatus status, StudyMemberRole role) {
        StudyMember member = mock(StudyMember.class);
        given(member.getStudyMemberId()).willReturn(memberId);
        given(member.getStudy()).willReturn(study);
        given(member.getStatus()).willReturn(status);
        given(member.getStudyMemberRole()).willReturn(role);
        given(member.getJoinedAt()).willReturn(LocalDateTime.now().minusDays(30));
        given(member.isActive()).willReturn(status == StudyMemberStatus.ACTIVE);
        return member;
    }

    private StudyMember mockMemberNoStudy(Long memberId, StudyMemberStatus status) {
        StudyMember member = mock(StudyMember.class);
        given(member.getStudyMemberId()).willReturn(memberId);
        given(member.getStatus()).willReturn(status);
        given(member.isActive()).willReturn(status == StudyMemberStatus.ACTIVE);
        return member;
    }
}
