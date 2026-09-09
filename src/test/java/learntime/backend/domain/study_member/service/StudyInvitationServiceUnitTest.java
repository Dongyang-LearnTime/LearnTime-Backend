package learntime.backend.domain.study_member.service;

import learntime.backend.domain.relationship.repository.FriendRepository;
import learntime.backend.domain.study.error.code.StudyErrorCode;
import learntime.backend.domain.study.error.exception.StudyException;
import learntime.backend.domain.study.model.Study;
import learntime.backend.domain.study.repository.StudyRepository;
import learntime.backend.domain.study_member.enums.StudyInvitationStatus;
import learntime.backend.domain.study_member.enums.StudyMemberStatus;
import learntime.backend.domain.study_member.event.StudyInvitationAcceptedEvent;
import learntime.backend.domain.study_member.model.StudyInvitation;
import learntime.backend.domain.study_member.model.StudyMember;
import learntime.backend.domain.study_member.repository.StudyInvitationRepository;
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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class StudyInvitationServiceUnitTest {

    @Mock
    private StudyMemberRepository studyMemberRepository;

    @Mock
    private StudyInvitationRepository studyInvitationRepository;

    @Mock
    private StudyRepository studyRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private FriendRepository friendRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private UserBlockUtil userBlockUtil;

    @InjectMocks
    private StudyInvitationService studyInvitationService;

    private StudyInvitation createMockInvitation(Long invitationId, Long studyId, Long inviterId, Long invitedId) {
        Study study = mock(Study.class);
        given(study.getStudyId()).willReturn(studyId);
        given(study.getStudyTitle()).willReturn("스프링 완전 정복 스터디");

        User inviter = mock(User.class);
        given(inviter.getUserId()).willReturn(inviterId);
        given(inviter.getName()).willReturn("방장");

        User invited = mock(User.class);
        given(invited.getUserId()).willReturn(invitedId);
        given(invited.getName()).willReturn("초대받은자");

        return StudyInvitation.builder()
                .study(study)
                .inviterUser(inviter)
                .invitedUser(invited)
                .build();
    }

    @Test
    @DisplayName("초대 수락 성공 - 비관적 락을 획득하고 멤버로 등록된다")
    void approveRequest_Success() {
        // given
        Long invitationId = 100L;
        Long studyId = 10L;
        Long inviterId = 1L;
        Long invitedId = 2L;

        StudyInvitation invitation = createMockInvitation(invitationId, studyId, inviterId, invitedId);
        given(studyInvitationRepository.findByIdWithPessimisticLock(invitationId))
                .willReturn(Optional.of(invitation));

        given(studyMemberRepository.existsByStudy_StudyIdAndUser_UserId(studyId, invitedId))
                .willReturn(false);

        Study study = invitation.getStudy();
        given(studyRepository.findByIdWithPessimisticLock(studyId))
                .willReturn(Optional.of(study));

        given(studyMemberRepository.countByStudyAndStatusIn(study, List.of(StudyMemberStatus.ACTIVE, StudyMemberStatus.COMPLETED)))
                .willReturn(2L);

        // when
        studyInvitationService.approveRequest(invitationId, invitedId);

        // then
        assertThat(invitation.getStatus()).isEqualTo(StudyInvitationStatus.ACCEPTED);
        verify(studyInvitationRepository).findByIdWithPessimisticLock(invitationId);
        verify(studyRepository).findByIdWithPessimisticLock(studyId);
        verify(studyMemberRepository).save(any(StudyMember.class));
        verify(eventPublisher).publishEvent(any(StudyInvitationAcceptedEvent.class));
    }

    @Test
    @DisplayName("초대 수락 실패 - 존재하지 않는 초대 ID")
    void approveRequest_Fail_InvitationNotFound() {
        // given
        Long invitationId = 999L;
        Long userId = 2L;

        given(studyInvitationRepository.findByIdWithPessimisticLock(invitationId))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> studyInvitationService.approveRequest(invitationId, userId))
                .isInstanceOf(StudyException.class)
                .hasMessageContaining(StudyErrorCode.STUDY_INVITATION_NOT_FOUND.getMessage());

        verify(studyMemberRepository, never()).save(any(StudyMember.class));
    }

    @Test
    @DisplayName("초대 수락 실패 - 이미 수락 또는 거절되어 PENDING 상태가 아닌 경우")
    void approveRequest_Fail_NotPending() {
        // given
        Long invitationId = 100L;
        Long studyId = 10L;
        Long inviterId = 1L;
        Long invitedId = 2L;

        StudyInvitation invitation = createMockInvitation(invitationId, studyId, inviterId, invitedId);
        invitation.accept(); // 이미 수락됨

        given(studyInvitationRepository.findByIdWithPessimisticLock(invitationId))
                .willReturn(Optional.of(invitation));

        // when & then
        assertThatThrownBy(() -> studyInvitationService.approveRequest(invitationId, invitedId))
                .isInstanceOf(StudyException.class)
                .hasMessageContaining(StudyErrorCode.STUDY_INVITATION_NOT_PENDING.getMessage());

        verify(studyMemberRepository, never()).save(any(StudyMember.class));
    }

    @Test
    @DisplayName("초대 수락 실패 - 초대받은 대상이 아닌 다른 사용자가 수락 시도 시 예외 발생")
    void approveRequest_Fail_NotInvitedUser() {
        // given
        Long invitationId = 100L;
        Long studyId = 10L;
        Long inviterId = 1L;
        Long invitedId = 2L;
        Long strangerId = 3L;

        StudyInvitation invitation = createMockInvitation(invitationId, studyId, inviterId, invitedId);
        given(studyInvitationRepository.findByIdWithPessimisticLock(invitationId))
                .willReturn(Optional.of(invitation));

        // when & then
        assertThatThrownBy(() -> studyInvitationService.approveRequest(invitationId, strangerId))
                .isInstanceOf(StudyException.class)
                .hasMessageContaining(StudyErrorCode.NOT_INVITED_USER.getMessage());

        verify(studyMemberRepository, never()).save(any(StudyMember.class));
    }

    @Test
    @DisplayName("초대 수락 실패 - 이미 해당 스터디의 멤버인 경우 예외 발생")
    void approveRequest_Fail_AlreadyMember() {
        // given
        Long invitationId = 100L;
        Long studyId = 10L;
        Long inviterId = 1L;
        Long invitedId = 2L;

        StudyInvitation invitation = createMockInvitation(invitationId, studyId, inviterId, invitedId);
        given(studyInvitationRepository.findByIdWithPessimisticLock(invitationId))
                .willReturn(Optional.of(invitation));

        // 이미 멤버로 등록되어 있음
        given(studyMemberRepository.existsByStudy_StudyIdAndUser_UserId(studyId, invitedId))
                .willReturn(true);

        // when & then
        assertThatThrownBy(() -> studyInvitationService.approveRequest(invitationId, invitedId))
                .isInstanceOf(StudyException.class)
                .hasMessageContaining(StudyErrorCode.ALREADY_STUDY_MEMBER.getMessage());

        verify(studyRepository, never()).findByIdWithPessimisticLock(anyLong());
        verify(studyMemberRepository, never()).save(any(StudyMember.class));
    }

    @Test
    @DisplayName("초대 수락 실패 - 스터디 최대 인원(4명) 초과 시 예외 발생")
    void approveRequest_Fail_MemberLimitExceeded() {
        // given
        Long invitationId = 100L;
        Long studyId = 10L;
        Long inviterId = 1L;
        Long invitedId = 2L;

        StudyInvitation invitation = createMockInvitation(invitationId, studyId, inviterId, invitedId);
        given(studyInvitationRepository.findByIdWithPessimisticLock(invitationId))
                .willReturn(Optional.of(invitation));

        given(studyMemberRepository.existsByStudy_StudyIdAndUser_UserId(studyId, invitedId))
                .willReturn(false);

        Study study = invitation.getStudy();
        given(studyRepository.findByIdWithPessimisticLock(studyId))
                .willReturn(Optional.of(study));

        // 이미 4명 차 있음
        given(studyMemberRepository.countByStudyAndStatusIn(study, List.of(StudyMemberStatus.ACTIVE, StudyMemberStatus.COMPLETED)))
                .willReturn(4L);

        // when & then
        assertThatThrownBy(() -> studyInvitationService.approveRequest(invitationId, invitedId))
                .isInstanceOf(StudyException.class)
                .hasMessageContaining(StudyErrorCode.STUDY_MEMBER_LIMIT_EXCEEDED.getMessage());

        verify(studyMemberRepository, never()).save(any(StudyMember.class));
        assertThat(invitation.getStatus()).isEqualTo(StudyInvitationStatus.PENDING);
    }

    @Test
    @DisplayName("동시 수락 시나리오 시뮬레이션 - 첫 번째 요청만 성공하고 두 번째 요청은 STUDY_INVITATION_NOT_PENDING 예외 발생")
    void approveRequest_ConcurrentExecution_OnlyOneSucceeds() throws InterruptedException {
        // given
        Long invitationId = 100L;
        Long studyId = 10L;
        Long inviterId = 1L;
        Long invitedId = 2L;

        StudyInvitation invitation = createMockInvitation(invitationId, studyId, inviterId, invitedId);

        // 비관적 락으로 인해 순차적으로 접근: 동일한 invitation 인스턴스를 공유하므로 첫 번째 트랜잭션이 accept()한 후 두 번째 트랜잭션은 ACCEPTED 상태를 보게 됨
        given(studyInvitationRepository.findByIdWithPessimisticLock(invitationId))
                .willReturn(Optional.of(invitation));

        given(studyMemberRepository.existsByStudy_StudyIdAndUser_UserId(studyId, invitedId))
                .willReturn(false);

        Study study = invitation.getStudy();
        given(studyRepository.findByIdWithPessimisticLock(studyId))
                .willReturn(Optional.of(study));

        given(studyMemberRepository.countByStudyAndStatusIn(study, List.of(StudyMemberStatus.ACTIVE, StudyMemberStatus.COMPLETED)))
                .willReturn(2L);

        int numberOfThreads = 2;
        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch latch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(numberOfThreads);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger notPendingErrorCount = new AtomicInteger(0);

        for (int i = 0; i < numberOfThreads; i++) {
            executorService.submit(() -> {
                try {
                    latch.await();
                    studyInvitationService.approveRequest(invitationId, invitedId);
                    successCount.incrementAndGet();
                } catch (StudyException e) {
                    if (e.getErrorCode() == StudyErrorCode.STUDY_INVITATION_NOT_PENDING) {
                        notPendingErrorCount.incrementAndGet();
                    }
                } catch (Exception ignored) {
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        // when: 두 스레드 동시 시작
        latch.countDown();
        doneLatch.await();
        executorService.shutdown();

        // then: 정확히 1번만 성공하고 1번은 STUDY_INVITATION_NOT_PENDING 예외 발생
        assertThat(successCount.get()).isEqualTo(1);
        assertThat(notPendingErrorCount.get()).isEqualTo(1);
        verify(studyMemberRepository, times(1)).save(any(StudyMember.class));
    }
}
