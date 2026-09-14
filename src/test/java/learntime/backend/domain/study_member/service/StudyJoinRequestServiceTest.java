package learntime.backend.domain.study_member.service;

import learntime.backend.domain.study.error.code.StudyErrorCode;
import learntime.backend.domain.study.error.exception.StudyException;
import learntime.backend.domain.study.model.Study;
import learntime.backend.domain.study.repository.StudyRepository;
import learntime.backend.domain.study_member.dto.response.StudyJoinRequestResponseDTO;
import learntime.backend.domain.study_member.enums.StudyJoinRequestStatus;
import learntime.backend.domain.study_member.enums.StudyMemberRole;
import learntime.backend.domain.study_member.enums.StudyMemberStatus;
import learntime.backend.domain.study_member.event.StudyJoinRequestApprovedEvent;
import learntime.backend.domain.study_member.event.StudyJoinRequestCreatedEvent;
import learntime.backend.domain.study_member.event.StudyJoinRequestRejectedEvent;
import learntime.backend.domain.study_member.model.StudyJoinRequest;
import learntime.backend.domain.study_member.model.StudyMember;
import learntime.backend.domain.study_member.repository.StudyJoinRequestRepository;
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
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class StudyJoinRequestServiceTest {

    @Mock
    private StudyJoinRequestRepository studyJoinRequestRepository;

    @Mock
    private StudyRepository studyRepository;

    @Mock
    private StudyMemberRepository studyMemberRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserBlockUtil userBlockUtil;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private StudyJoinRequestService studyJoinRequestService;

    @Test
    @DisplayName("스터디 가입 요청 성공")
    void requestJoin_Success() {
        // given
        Long userId = 2L;
        Long ownerId = 1L;
        Long studyId = 10L;

        User applicant = mock(User.class);
        given(applicant.getUserId()).willReturn(userId);
        given(applicant.getName()).willReturn("지원자");
        given(userRepository.findById(userId)).willReturn(Optional.of(applicant));

        Study study = mock(Study.class);
        given(study.getStudyId()).willReturn(studyId);
        given(study.getStudyTitle()).willReturn("스프링 스터디");
        given(study.getIsPublic()).willReturn(true);
        given(studyRepository.findById(studyId)).willReturn(Optional.of(study));

        given(studyMemberRepository.countByStudyAndStatusIn(study, List.of(StudyMemberStatus.ACTIVE, StudyMemberStatus.COMPLETED)))
                .willReturn(2L);
        given(studyMemberRepository.existsByStudy_StudyIdAndUser_UserId(studyId, userId)).willReturn(false);
        given(studyJoinRequestRepository.existsByStudy_StudyIdAndRequesterUser_UserIdAndStatus(studyId, userId, StudyJoinRequestStatus.PENDING))
                .willReturn(false);

        User ownerUser = mock(User.class);
        given(ownerUser.getUserId()).willReturn(ownerId);
        StudyMember ownerMember = mock(StudyMember.class);
        given(ownerMember.getUser()).willReturn(ownerUser);
        given(studyMemberRepository.findByStudy_StudyIdAndStudyMemberRoleAndStatus(studyId, StudyMemberRole.OWNER, StudyMemberStatus.ACTIVE))
                .willReturn(Optional.of(ownerMember));

        StudyJoinRequest savedReq = mock(StudyJoinRequest.class);
        given(savedReq.getStudyJoinRequestId()).willReturn(100L);
        given(studyJoinRequestRepository.save(any(StudyJoinRequest.class))).willReturn(savedReq);

        // when
        Long requestId = studyJoinRequestService.requestJoin(studyId, userId);

        // then
        assertThat(requestId).isEqualTo(100L);
        verify(userBlockUtil).validateNotBlockedByUser(ownerId, userId);
        verify(studyJoinRequestRepository).save(any(StudyJoinRequest.class));
        verify(eventPublisher).publishEvent(any(StudyJoinRequestCreatedEvent.class));
    }

    @Test
    @DisplayName("스터디 가입 요청 실패 - 비공개 스터디")
    void requestJoin_Fail_NotPublic() {
        // given
        Long userId = 2L;
        Long studyId = 10L;

        User applicant = mock(User.class);
        given(userRepository.findById(userId)).willReturn(Optional.of(applicant));

        Study study = mock(Study.class);
        given(study.getIsPublic()).willReturn(false);
        given(studyRepository.findById(studyId)).willReturn(Optional.of(study));

        // when & then
        assertThatThrownBy(() -> studyJoinRequestService.requestJoin(studyId, userId))
                .isInstanceOf(StudyException.class)
                .hasMessageContaining(StudyErrorCode.STUDY_NOT_PUBLIC.getMessage());
    }

    @Test
    @DisplayName("스터디 가입 요청 실패 - 정원 초과")
    void requestJoin_Fail_LimitExceeded() {
        // given
        Long userId = 2L;
        Long studyId = 10L;

        User applicant = mock(User.class);
        given(userRepository.findById(userId)).willReturn(Optional.of(applicant));

        Study study = mock(Study.class);
        given(study.getIsPublic()).willReturn(true);
        given(studyRepository.findById(studyId)).willReturn(Optional.of(study));

        given(studyMemberRepository.countByStudyAndStatusIn(study, List.of(StudyMemberStatus.ACTIVE, StudyMemberStatus.COMPLETED)))
                .willReturn(4L);

        // when & then
        assertThatThrownBy(() -> studyJoinRequestService.requestJoin(studyId, userId))
                .isInstanceOf(StudyException.class)
                .hasMessageContaining(StudyErrorCode.STUDY_MEMBER_LIMIT_EXCEEDED.getMessage());
    }

    @Test
    @DisplayName("스터디 가입 요청 실패 - 이미 대기 중인 요청 존재")
    void requestJoin_Fail_AlreadyRequested() {
        // given
        Long userId = 2L;
        Long studyId = 10L;

        User applicant = mock(User.class);
        given(userRepository.findById(userId)).willReturn(Optional.of(applicant));

        Study study = mock(Study.class);
        given(study.getStudyId()).willReturn(studyId);
        given(study.getIsPublic()).willReturn(true);
        given(studyRepository.findById(studyId)).willReturn(Optional.of(study));

        given(studyMemberRepository.countByStudyAndStatusIn(study, List.of(StudyMemberStatus.ACTIVE, StudyMemberStatus.COMPLETED)))
                .willReturn(1L);
        given(studyMemberRepository.existsByStudy_StudyIdAndUser_UserId(studyId, userId)).willReturn(false);
        given(studyJoinRequestRepository.existsByStudy_StudyIdAndRequesterUser_UserIdAndStatus(studyId, userId, StudyJoinRequestStatus.PENDING))
                .willReturn(true);

        // when & then
        assertThatThrownBy(() -> studyJoinRequestService.requestJoin(studyId, userId))
                .isInstanceOf(StudyException.class)
                .hasMessageContaining(StudyErrorCode.ALREADY_JOIN_REQUESTED.getMessage());
    }

    @Test
    @DisplayName("스터디 가입 요청 승인 성공 (방장)")
    void approveRequest_Success() {
        // given
        Long requestId = 100L;
        Long ownerId = 1L;
        Long requesterId = 2L;
        Long studyId = 10L;

        Study study = mock(Study.class);
        given(study.getStudyId()).willReturn(studyId);
        given(study.getStudyTitle()).willReturn("스프링 스터디");

        User requester = mock(User.class);
        given(requester.getUserId()).willReturn(requesterId);

        StudyJoinRequest joinRequest = StudyJoinRequest.builder()
                .study(study)
                .requesterUser(requester)
                .build();

        given(studyJoinRequestRepository.findByIdWithDetails(requestId)).willReturn(Optional.of(joinRequest));

        StudyMember ownerMember = mock(StudyMember.class);
        given(ownerMember.isActive()).willReturn(true);
        given(ownerMember.getStudyMemberRole()).willReturn(StudyMemberRole.OWNER);
        given(studyMemberRepository.findByStudy_StudyIdAndUser_UserIdAndStatus(studyId, ownerId, StudyMemberStatus.ACTIVE))
                .willReturn(Optional.of(ownerMember));

        given(studyRepository.findByIdWithPessimisticLock(studyId)).willReturn(Optional.of(study));
        given(studyMemberRepository.countByStudyAndStatusIn(study, List.of(StudyMemberStatus.ACTIVE, StudyMemberStatus.COMPLETED)))
                .willReturn(2L);
        given(studyMemberRepository.existsByStudy_StudyIdAndUser_UserId(studyId, requesterId)).willReturn(false);

        StudyMember savedMember = mock(StudyMember.class);
        given(savedMember.getStudyMemberId()).willReturn(500L);
        given(studyMemberRepository.save(any(StudyMember.class))).willReturn(savedMember);

        // when
        Long memberId = studyJoinRequestService.approveRequest(requestId, ownerId);

        // then
        assertThat(memberId).isEqualTo(500L);
        assertThat(joinRequest.getStatus()).isEqualTo(StudyJoinRequestStatus.APPROVED);
        verify(studyMemberRepository).save(any(StudyMember.class));
        verify(eventPublisher).publishEvent(any(StudyJoinRequestApprovedEvent.class));
    }

    @Test
    @DisplayName("스터디 가입 요청 거절 성공 (방장)")
    void rejectRequest_Success() {
        // given
        Long requestId = 100L;
        Long ownerId = 1L;
        Long requesterId = 2L;
        Long studyId = 10L;

        Study study = mock(Study.class);
        given(study.getStudyId()).willReturn(studyId);
        given(study.getStudyTitle()).willReturn("스프링 스터디");

        User requester = mock(User.class);
        given(requester.getUserId()).willReturn(requesterId);

        StudyJoinRequest joinRequest = StudyJoinRequest.builder()
                .study(study)
                .requesterUser(requester)
                .build();

        given(studyJoinRequestRepository.findByIdWithDetails(requestId)).willReturn(Optional.of(joinRequest));

        StudyMember ownerMember = mock(StudyMember.class);
        given(ownerMember.isActive()).willReturn(true);
        given(ownerMember.getStudyMemberRole()).willReturn(StudyMemberRole.OWNER);
        given(studyMemberRepository.findByStudy_StudyIdAndUser_UserIdAndStatus(studyId, ownerId, StudyMemberStatus.ACTIVE))
                .willReturn(Optional.of(ownerMember));

        // when
        studyJoinRequestService.rejectRequest(requestId, ownerId);

        // then
        assertThat(joinRequest.getStatus()).isEqualTo(StudyJoinRequestStatus.REJECTED);
        verify(eventPublisher).publishEvent(any(StudyJoinRequestRejectedEvent.class));
    }

    @Test
    @DisplayName("스터디 가입 요청 취소 성공 (요청자)")
    void cancelRequest_Success() {
        // given
        Long requestId = 100L;
        Long requesterId = 2L;
        Long studyId = 10L;

        Study study = mock(Study.class);
        User requester = mock(User.class);
        given(requester.getUserId()).willReturn(requesterId);

        StudyJoinRequest joinRequest = StudyJoinRequest.builder()
                .study(study)
                .requesterUser(requester)
                .build();

        given(studyJoinRequestRepository.findByIdWithDetails(requestId)).willReturn(Optional.of(joinRequest));

        // when
        studyJoinRequestService.cancelRequest(requestId, requesterId);

        // then
        assertThat(joinRequest.getStatus()).isEqualTo(StudyJoinRequestStatus.CANCELED);
    }
}
