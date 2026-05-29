package learntime.backend.domain.profile.service;

import learntime.backend.domain.relationship.model.Friend;
import learntime.backend.domain.relationship.model.FriendRequest;
import learntime.backend.domain.relationship.repository.FriendRepository;
import learntime.backend.domain.relationship.repository.FriendRequestRepository;
import learntime.backend.domain.profile.dto.response.ProfileResponseDTO;
import learntime.backend.domain.profile.enums.ProfileVisibility;
import learntime.backend.domain.profile.model.Profile;
import learntime.backend.domain.profile.repository.ProfileRepository;
import learntime.backend.domain.user.enums.AuthProvider;
import learntime.backend.domain.user.enums.Role;
import learntime.backend.domain.user.model.User;
import learntime.backend.domain.user.repository.UserRepository;
import learntime.backend.global.error.exception.AuthException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
class ProfileServiceTest {

    @Autowired
    private ProfileService profileService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProfileRepository profileRepository;

    @Autowired
    private FriendRepository friendRepository;

    @Autowired
    private FriendRequestRepository friendRequestRepository;

    private User createUser(String name, String email) {
        return userRepository.save(
                User.builder()
                        .email(email)
                        .password("password123!")
                        .name(name)
                        .socialId(email)
                        .socialProvider(AuthProvider.LOCAL)
                        .role(Role.ROLE_USER)
                        .build()
        );
    }

    private Profile createProfile(User user, ProfileVisibility visibility) {
        return profileRepository.save(
                Profile.builder()
                        .user(user)
                        .description("Hello")
                        .profileVisibility(visibility)
                        .profileImageUrl("image.png")
                        .build()
        );
    }

    @Test
    @DisplayName("프로필 조회 시 로그인 사용자와 대상 사용자가 친구인 경우 isFriend가 true로 반환된다")
    void getProfile_isFriend_true() {
        // given
        User target = createUser("대상", "target@test.com");
        User loginUser = createUser("로그인유저", "login@test.com");
        createProfile(target, ProfileVisibility.PUBLIC);

        Friend friend = Friend.builder()
                .user(target)
                .friendUser(loginUser)
                .build();
        friendRepository.save(friend);

        // when
        ProfileResponseDTO response = profileService.getProfile(target.getUserId(), loginUser.getUserId());

        // then
        assertThat(response.isFriend()).isTrue();
    }

    @Test
    @DisplayName("프로필 조회 시 로그인 사용자와 대상 사용자가 친구가 아닌 경우 isFriend가 false로 반환된다")
    void getProfile_isFriend_false() {
        // given
        User target = createUser("대상", "target@test.com");
        User loginUser = createUser("로그인유저", "login@test.com");
        createProfile(target, ProfileVisibility.PUBLIC);

        // when
        ProfileResponseDTO response = profileService.getProfile(target.getUserId(), loginUser.getUserId());

        // then
        assertThat(response.isFriend()).isFalse();
    }

    @Test
    @DisplayName("로그아웃 상태(currentUserId가 null)로 프로필을 조회하면 isFriend가 false로 반환된다")
    void getProfile_loggedOut_isFriend_false() {
        // given
        User target = createUser("대상", "target@test.com");
        createProfile(target, ProfileVisibility.PUBLIC);

        // when
        ProfileResponseDTO response = profileService.getProfile(target.getUserId(), null);

        // then
        assertThat(response.isFriend()).isFalse();
        assertThat(response.hasPendingSentRequest()).isFalse();
        assertThat(response.hasPendingReceivedRequest()).isFalse();
        assertThat(response.pendingFriendRequestId()).isNull();
    }

    @Test
    @DisplayName("비공개 프로필을 타인이 조회하면 UNAUTHORIZED_ACCESS 에러가 발생한다")
    void getProfile_private_accessDenied() {
        // given
        User target = createUser("대상", "target@test.com");
        User other = createUser("타인", "other@test.com");
        createProfile(target, ProfileVisibility.PRIVATE);

        // when & then
        assertThatThrownBy(() -> profileService.getProfile(target.getUserId(), other.getUserId()))
                .isInstanceOf(AuthException.class);
    }

    @Test
    @DisplayName("로그인 유저가 대상 유저에게 보낸 대기 중인 친구 요청이 존재하면 hasPendingSentRequest가 true이고 request id가 반환된다")
    void getProfile_pendingSentRequest_true() {
        // given
        User target = createUser("대상", "target@test.com");
        User loginUser = createUser("로그인유저", "login@test.com");
        createProfile(target, ProfileVisibility.PUBLIC);

        FriendRequest request = FriendRequest.builder()
                .requester(loginUser)
                .receiver(target)
                .status(learntime.backend.domain.user.enums.FriendRequestStatus.PENDING)
                .build();
        friendRequestRepository.save(request);

        // when
        ProfileResponseDTO response = profileService.getProfile(target.getUserId(), loginUser.getUserId());

        // then
        assertThat(response.isFriend()).isFalse();
        assertThat(response.hasPendingSentRequest()).isTrue();
        assertThat(response.hasPendingReceivedRequest()).isFalse();
        assertThat(response.pendingFriendRequestId()).isEqualTo(request.getFriendRequestId());
    }

    @Test
    @DisplayName("대상 유저가 로그인 유저에게 보낸 대기 중인 친구 요청이 존재하면 hasPendingReceivedRequest가 true이고 request id가 반환된다")
    void getProfile_pendingReceivedRequest_true() {
        // given
        User target = createUser("대상", "target@test.com");
        User loginUser = createUser("로그인유저", "login@test.com");
        createProfile(target, ProfileVisibility.PUBLIC);

        FriendRequest request = FriendRequest.builder()
                .requester(target)
                .receiver(loginUser)
                .status(learntime.backend.domain.user.enums.FriendRequestStatus.PENDING)
                .build();
        friendRequestRepository.save(request);

        // when
        ProfileResponseDTO response = profileService.getProfile(target.getUserId(), loginUser.getUserId());

        // then
        assertThat(response.isFriend()).isFalse();
        assertThat(response.hasPendingSentRequest()).isFalse();
        assertThat(response.hasPendingReceivedRequest()).isTrue();
        assertThat(response.pendingFriendRequestId()).isEqualTo(request.getFriendRequestId());
    }
}
