package learntime.backend.domain.study.service.core;

import learntime.backend.domain.relationship.repository.FriendRepository;
import learntime.backend.domain.study.error.code.StudyErrorCode;
import learntime.backend.domain.study.error.exception.StudyException;
import learntime.backend.domain.study.model.Study;
import learntime.backend.domain.study.repository.StudyRepository;
import learntime.backend.domain.study_member.model.StudyInvitation;
import learntime.backend.domain.study_member.model.StudyMember;
import learntime.backend.domain.study_member.repository.StudyInvitationRepository;
import learntime.backend.domain.study_member.repository.StudyMemberRepository;
import learntime.backend.domain.study_plan.dto.request.GeminiStudyRequestDTO;
import learntime.backend.domain.study_plan.service.core.StudyRestManager;
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

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class StudyInitializationServiceTest {

    @Mock
    private StudyRepository studyRepository;

    @Mock
    private StudyMemberRepository studyMemberRepository;

    @Mock
    private StudyInvitationRepository studyInvitationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private FriendRepository friendRepository;

    @Mock
    private UserBlockUtil userBlockUtil;

    @Mock
    private StudyRestManager studyRestManager;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private StudyInitializationService studyInitializationService;

    @Test
    @DisplayName("비공개 스터디 생성 성공 - 친구 관계 검증 통과 및 초대장 발송")
    void initializeStudy_Private_Success() {
        // given
        Long userId = 1L;
        Long friendId = 2L;

        User user = mock(User.class);
        given(user.getUserId()).willReturn(userId);
        given(user.getName()).willReturn("방장");
        given(userRepository.findById(userId)).willReturn(Optional.of(user));

        User friend = mock(User.class);
        given(friend.getUserId()).willReturn(friendId);
        given(userRepository.findAllById(List.of(friendId))).willReturn(List.of(friend));

        given(friendRepository.existsFriendRelation(userId, friendId)).willReturn(true);

        GeminiStudyRequestDTO request = new GeminiStudyRequestDTO(
                "교재",
                "비공개 스터디",
                LocalDate.now(),
                LocalDate.now().plusDays(20),
                List.of(),
                List.of(friendId),
                List.of(),
                List.of(),
                false // 비공개
        );

        StudyInvitation invitation = mock(StudyInvitation.class);
        given(invitation.getStudyInvitationId()).willReturn(100L);
        given(invitation.getInvitedUser()).willReturn(friend);
        given(studyInvitationRepository.saveAll(anyList())).willReturn(List.of(invitation));

        // when
        Long studyId = studyInitializationService.initializeStudy(request, userId);

        // then
        verify(userBlockUtil).validateNotBlockedByUser(userId, friendId);
        verify(friendRepository).existsFriendRelation(userId, friendId);
        verify(studyRepository).save(any(Study.class));
        verify(studyMemberRepository).save(any(StudyMember.class));
        verify(studyInvitationRepository).saveAll(anyList());
    }

    @Test
    @DisplayName("비공개 스터디 생성 실패 - 친구 관계가 아닌 유저 초대 시 예외 발생")
    void initializeStudy_Private_Fail_NotFriend() {
        // given
        Long userId = 1L;
        Long strangerId = 99L;

        User user = mock(User.class);
        given(user.getUserId()).willReturn(userId);
        given(userRepository.findById(userId)).willReturn(Optional.of(user));

        User stranger = mock(User.class);
        given(stranger.getUserId()).willReturn(strangerId);
        given(userRepository.findAllById(List.of(strangerId))).willReturn(List.of(stranger));

        given(friendRepository.existsFriendRelation(userId, strangerId)).willReturn(false);

        GeminiStudyRequestDTO request = new GeminiStudyRequestDTO(
                "교재",
                "비공개 스터디",
                LocalDate.now(),
                LocalDate.now().plusDays(20),
                List.of(),
                List.of(strangerId),
                List.of(),
                List.of(),
                false // 비공개
        );

        // when & then
        assertThatThrownBy(() -> studyInitializationService.initializeStudy(request, userId))
                .isInstanceOf(StudyException.class);

        verify(userBlockUtil).validateNotBlockedByUser(userId, strangerId);
        verify(friendRepository).existsFriendRelation(userId, strangerId);
        verify(studyInvitationRepository, never()).saveAll(anyList());
    }

    @Test
    @DisplayName("공개 스터디 생성 성공 - 친구가 아니어도 초대 가능")
    void initializeStudy_Public_Success() {
        // given
        Long userId = 1L;
        Long otherUserId = 99L;

        User user = mock(User.class);
        given(user.getUserId()).willReturn(userId);
        given(user.getName()).willReturn("방장");
        given(userRepository.findById(userId)).willReturn(Optional.of(user));

        User otherUser = mock(User.class);
        given(otherUser.getUserId()).willReturn(otherUserId);
        given(userRepository.findAllById(List.of(otherUserId))).willReturn(List.of(otherUser));

        GeminiStudyRequestDTO request = new GeminiStudyRequestDTO(
                "교재",
                "공개 스터디",
                LocalDate.now(),
                LocalDate.now().plusDays(20),
                List.of(),
                List.of(otherUserId),
                List.of(),
                List.of(),
                true // 공개
        );

        StudyInvitation invitation = mock(StudyInvitation.class);
        given(invitation.getStudyInvitationId()).willReturn(101L);
        given(invitation.getInvitedUser()).willReturn(otherUser);
        given(studyInvitationRepository.saveAll(anyList())).willReturn(List.of(invitation));

        // when
        Long studyId = studyInitializationService.initializeStudy(request, userId);

        // then
        verify(userBlockUtil).validateNotBlockedByUser(userId, otherUserId);
        verify(friendRepository, never()).existsFriendRelation(anyLong(), anyLong());
        verify(studyRepository).save(any(Study.class));
    }
}
