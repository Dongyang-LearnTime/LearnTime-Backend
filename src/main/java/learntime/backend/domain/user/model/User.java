package learntime.backend.domain.user.model;

import jakarta.persistence.*;
import learntime.backend.domain.badge.model.UserActivityStat;
import learntime.backend.domain.badge.model.UserBadge;
import learntime.backend.domain.calendar.model.CalendarRecord;
import learntime.backend.domain.community.model.Comment;
import learntime.backend.domain.community.model.Post;
import learntime.backend.domain.community.model.PostLike;
import learntime.backend.domain.exercise.model.ExerciseRecord;
import learntime.backend.domain.exercise.model.MealRecord;
import learntime.backend.domain.exercise.model.WeightRecord;
import learntime.backend.domain.relationship.model.Friend;
import learntime.backend.domain.relationship.model.FriendRequest;
import learntime.backend.domain.relationship.model.UserBlock;
import learntime.backend.domain.message.model.Message;
import learntime.backend.domain.notification.model.Notification;
import learntime.backend.domain.point.model.PointHistory;
import learntime.backend.domain.profile.model.Profile;
import learntime.backend.domain.study_member.model.StudyMember;
import learntime.backend.domain.study_member.model.StudyInvitation;
import learntime.backend.domain.user.enums.AuthProvider;
import learntime.backend.domain.user.enums.Role;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(uniqueConstraints = {
        @UniqueConstraint(name = "uq_social_user", columnNames = {"social_id", "social_provider"})
})
@SQLDelete(sql = "UPDATE user SET deleted_at = NOW() WHERE user_id = ?") // DELECT 수행 시, 삭제 대신 deletedAt에 시간 표시(soft delete)
@SQLRestriction("deleted_at = '1970-01-01 00:00:00'") // 삭제 처리가 안된 데이터만 기본적으로 조회
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userId;

    @Column(nullable = false, unique = true)
    private String email;

    @Column
    private String password; // OAuth인 경우 null

    @Column(nullable = false, length = 30, unique = true)
    private String name;

    @Column(nullable = false, columnDefinition = "INT DEFAULT 0 CHECK (point >= 0)")
    private Integer point = 0;

    @Column(name = "social_id")
    private String socialId; // OAuth에서 제공하는 Id (사이트 가입인 경우 null)

    @Enumerated(EnumType.STRING)
    @Column(name = "social_provider", nullable = false)
    private AuthProvider socialProvider; // 어떤 경로로 가입했는지 (사이트 자체, 구글, 네이버)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role; // 권한 (사용자, 관리자)

    @CreatedDate
    @Column(updatable = false, nullable = false)
    private LocalDateTime createdAt; // 가입일

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt; // 정보 수정일

    // 삭제 처리된 날짜 (삭제되지 않은 유저인 경우 1970-01-01 로 NULL를 대체, MySQL의 NULL Unique 우회 문제 해결)
    @Column(nullable = false)
    private LocalDateTime deletedAt = LocalDateTime.of(1970, 1, 1, 0, 0);

    // 비밀번호 틀린 횟수
    @Column(name = "failed_attempts", nullable = false)
    private Integer failedAttempts = 0;

    // 계정 잠금 발생 시간 (NULL이면 잠기지 않은 상태)
    @Column(name = "locked_at")
    private LocalDateTime lockedAt;

    // ==================== 연관관계 매핑 ====================

    // 진도 맴버
    @OneToMany(mappedBy = "user", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    private List<StudyMember> studyMembers = new ArrayList<>();

    // 진도 초대 받은 사용자
    @OneToMany(mappedBy = "invitedUser", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    private List<StudyInvitation> receivedStudyInvitations = new ArrayList<>();

    // 진도 초대한 사용자
    @OneToMany(mappedBy = "inviterUser", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    private List<StudyInvitation> sentStudyInvitations = new ArrayList<>();

    // 운동 기록
    @OneToMany(mappedBy = "user", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    private List<ExerciseRecord> exerciseRecords = new ArrayList<>();

    // 신체 분석
    @OneToMany(mappedBy = "user", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    private List<WeightRecord> weightRecord = new ArrayList<>();

    // 식단
    @OneToMany(mappedBy = "user", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    private List<MealRecord> mealRecord = new ArrayList<>();

    // 캘린더
    @OneToMany(mappedBy = "user", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    private List<CalendarRecord> calendarRecord = new ArrayList<>();

    // 포인트 내역
    @OneToMany(mappedBy = "user", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    private List<PointHistory> pointHistories = new ArrayList<>();

    // 게시글 (SET NULL)
    @OneToMany(mappedBy = "user")
    private List<Post> posts = new ArrayList<>();

    // 댓글 (SET NULL)
    @OneToMany(mappedBy = "user")
    private List<Comment> comments = new ArrayList<>();

    // 게시글 좋아요 사용자 목록
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PostLike> postLikes = new ArrayList<>();

    // 리프레쉬 토큰
    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private RefreshToken refreshToken;

    // 토큰 할당량
    @OneToOne(mappedBy = "user", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    private PromptQuotas promptQuotas;

    // 약관 동의 여부
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UserTerms> userTerms = new ArrayList<>();

    // 친구 목록
    @OneToMany(mappedBy = "user", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    private List<Friend> friends = new ArrayList<>();

    // 보낸 친구 요청
    @OneToMany(mappedBy = "requester", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    private List<FriendRequest> sentFriendRequests = new ArrayList<>();

    // 받은 친구 요청
    @OneToMany(mappedBy = "receiver", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    private List<FriendRequest> receivedFriendRequests = new ArrayList<>();

    // 차단한 사용자
    @OneToMany(mappedBy = "blocker", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    private List<UserBlock> blockedUsers = new ArrayList<>();

    // 차단 당한 사용자
    @OneToMany(mappedBy = "blocked", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    private List<UserBlock> blockedByUsers = new ArrayList<>();

    // 보낸 쪽지
    @OneToMany(mappedBy = "sender", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    private List<Message> sentMessageList = new ArrayList<>();

    // 받은 쪽지
    @OneToMany(mappedBy = "receiver", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    private List<Message> receiverMessageList = new ArrayList<>();

    // 알림 목록
    @OneToMany(mappedBy = "receiver", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    private List<Notification> notifications = new ArrayList<>();

    // 획득한 배지
    @OneToMany(mappedBy = "user", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    private List<UserBadge> userBadges = new ArrayList<>();

    // 사용자 활동 통계
    @OneToMany(mappedBy = "user", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    private List<UserActivityStat> userActivityStats = new ArrayList<>();

    // 사용자 프로필
    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private Profile profile;


    @Builder
    public User(String email, String password, String name, String socialId, AuthProvider socialProvider, Role role) {
        this.email = email;
        this.password = password;
        this.name = name;
        this.socialId = socialId;
        this.socialProvider = (socialProvider == null) ? AuthProvider.LOCAL : socialProvider;
        this.role = (role == null) ? Role.ROLE_USER : role;
    }

    // ==================== 비밀번호 틀림 관련 로직 ====================

    // 로그인 실패 횟수 1 증가
    public void incrementFailedAttempts() {
        this.failedAttempts++;
    }

    // 로그인 성공 시 실패 횟수 및 잠금 상태 초기화
    public void resetFailedAttempts() {
        this.failedAttempts = 0;
        this.lockedAt = null;
    }

    // 5회 실패 시 계정 잠금 처리
    public void lockAccount() {
        this.lockedAt = LocalDateTime.now();
    }

    // 현재 계정이 잠겨있는지 확인
    public Boolean isAccountLocked() {
        final int LOCK_MINUTES = 30; // 잠금 시간 (30분)

        if (this.lockedAt != null) {
            // 잠금 시간 지났는지 확인
            if (this.lockedAt.plusMinutes(LOCK_MINUTES).isAfter(LocalDateTime.now())) {
                return true;
            } else {
                resetFailedAttempts();
                return false;
            }
        }
        return false;
    }

    // 닉네임(이름) 정보 수정
    public void updateInfo(String name) {
        if (name != null && !name.isBlank()) {
            this.name = name;
        }
    }

    //비밀번호 수정 (암호화된 비밀번호를 인자로 받음)
    public void updatePassword(String encodedPassword) {
        if (encodedPassword != null && !encodedPassword.isBlank()) {
            this.password = encodedPassword;
        }
    }

}
