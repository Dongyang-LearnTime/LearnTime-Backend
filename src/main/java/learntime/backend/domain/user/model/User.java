package learntime.backend.domain.user.model;

import jakarta.persistence.*;
import learntime.backend.domain.study.model.Study;
import learntime.backend.domain.study.model.StudyDailyPlan;
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
        @UniqueConstraint(name = "uq_user_email", columnNames = {"email", "deleted_at"}),
        @UniqueConstraint(name = "uq_user_name", columnNames = {"name", "deleted_at"}),
        @UniqueConstraint(name = "uq_social_user", columnNames = {"social_id", "social_provider", "deleted_at"})
})
@SQLDelete(sql = "UPDATE users SET deleted_at = NOW() WHERE user_id = ?") // DELECT 수행 시, 삭제 대신 deletedAt에 시간 표시(soft delete)
@SQLRestriction("deleted_at = '1970-01-01 00:00:00'") // 삭제 처리가 안된 데이터만 기본적으로 조회
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userId;

    @Column(nullable = false)
    private String email;

    @Column()
    private String password; // OAuth인 경우 null

    @Column(nullable = false, length = 30)
    private String name;

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

    // ==================== 연관관계 매핑 ====================

    // 진도
    @OneToMany(mappedBy = "user", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    private List<Study> study = new ArrayList<>();

    // 리프레쉬 토큰
    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private RefreshToken refreshToken;

    // 토큰 할당량
    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private PromptQuota promptQuota;

    @Builder
    public User(String email, String password, String name, String socialId, AuthProvider socialProvider, Role role) {
        this.email = email;
        this.password = password;
        this.name = name;
        this.socialId = socialId;
        this.socialProvider = (socialProvider == null) ? AuthProvider.LOCAL : socialProvider;
        this.role = (role == null) ? Role.ROLE_USER : role;
    }

    public enum AuthProvider { LOCAL, GOOGLE, NAVER }
    public enum Role { ROLE_USER, ROLE_ADMIN }
}
