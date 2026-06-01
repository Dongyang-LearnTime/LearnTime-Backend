package learntime.backend.domain.study.converter;

import learntime.backend.domain.study.dto.response.StudyArchiveResponseDTO;
import learntime.backend.domain.study.model.Study;
import learntime.backend.domain.study_member.enums.StudyMemberRole;
import learntime.backend.domain.study_member.enums.StudyMemberStatus;
import learntime.backend.domain.study_member.model.StudyMember;
import learntime.backend.global.error.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.mock;

@DisplayName("StudyArchiveConverter 단위 테스트")
class StudyArchiveConverterTest {

    // ──────────────────────────────────────────────────────────
    // toStudyArchiveResponseDTO — 컨버터 Builder 패턴 검증
    // ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("ACTIVE 멤버를 DTO로 변환하면 모든 필드가 올바르게 매핑된다")
    void toStudyArchiveResponseDTO_ActiveMember_MapsAllFields() {
        // given
        LocalDate startDate = LocalDate.of(2025, 1, 1);
        LocalDate endDate = LocalDate.of(2025, 3, 31);
        LocalDateTime joinedAt = LocalDateTime.of(2025, 1, 1, 9, 0);

        Study study = mock(Study.class);
        given(study.getStudyId()).willReturn(10L);
        given(study.getStudyTitle()).willReturn("Java 스터디");
        given(study.getBookTitle()).willReturn("Effective Java");
        given(study.getStartDate()).willReturn(startDate);
        given(study.getEndDate()).willReturn(endDate);

        StudyMember member = mock(StudyMember.class);
        given(member.getStudy()).willReturn(study);
        given(member.getStudyMemberRole()).willReturn(StudyMemberRole.OWNER);
        given(member.getStatus()).willReturn(StudyMemberStatus.ACTIVE);
        given(member.getJoinedAt()).willReturn(joinedAt);

        // when
        StudyArchiveResponseDTO dto = StudyArchiveConverter.toStudyArchiveResponseDTO(member);

        // then
        assertThat(dto.studyId()).isEqualTo(10L);
        assertThat(dto.studyTitle()).isEqualTo("Java 스터디");
        assertThat(dto.bookTitle()).isEqualTo("Effective Java");
        assertThat(dto.startDate()).isEqualTo(startDate);
        assertThat(dto.endDate()).isEqualTo(endDate);
        assertThat(dto.myRole()).isEqualTo(StudyMemberRole.OWNER);
        assertThat(dto.myStatus()).isEqualTo(StudyMemberStatus.ACTIVE);
        assertThat(dto.joinedAt()).isEqualTo(joinedAt);
    }

    @Test
    @DisplayName("WITHDRAWN 멤버를 DTO로 변환하면 myStatus가 WITHDRAWN으로 세팅된다")
    void toStudyArchiveResponseDTO_WithdrawnMember_StatusIsWithdrawn() {
        // given
        Study study = mock(Study.class);
        given(study.getStudyId()).willReturn(20L);
        given(study.getStudyTitle()).willReturn("탈퇴 스터디");
        given(study.getBookTitle()).willReturn("Clean Code");
        given(study.getStartDate()).willReturn(LocalDate.of(2024, 6, 1));
        given(study.getEndDate()).willReturn(LocalDate.of(2024, 8, 31));

        StudyMember member = mock(StudyMember.class);
        given(member.getStudy()).willReturn(study);
        given(member.getStudyMemberRole()).willReturn(StudyMemberRole.MEMBER);
        given(member.getStatus()).willReturn(StudyMemberStatus.WITHDRAWN);
        given(member.getJoinedAt()).willReturn(LocalDateTime.now().minusMonths(3));

        // when
        StudyArchiveResponseDTO dto = StudyArchiveConverter.toStudyArchiveResponseDTO(member);

        // then
        assertThat(dto.myStatus()).isEqualTo(StudyMemberStatus.WITHDRAWN);
        assertThat(dto.myRole()).isEqualTo(StudyMemberRole.MEMBER);
    }

    @Test
    @DisplayName("유틸리티 클래스는 직접 인스턴스화할 수 없다")
    void constructor_ThrowsBusinessException() {
        assertThatThrownBy(() -> {
            var ctor = StudyArchiveConverter.class.getDeclaredConstructor();
            ctor.setAccessible(true);
            ctor.newInstance();
        }).hasCauseInstanceOf(BusinessException.class);
    }
}
