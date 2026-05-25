package learntime.backend.domain.profile.model;

import jakarta.persistence.*;
import learntime.backend.domain.profile.enums.ProfileVisibility;
import learntime.backend.domain.user.model.User;
import lombok.*;

@Entity
@Table(name = "profile")
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Profile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long profileId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column
    private String profileImageUrl;

    @Column(length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ProfileVisibility profileVisibility = ProfileVisibility.PUBLIC;

    public void updateProfile(String description, ProfileVisibility profileVisibility, String profileImageUrl) {
        if (description != null) {
            this.description = description;
        }
        if (profileVisibility != null) {
            this.profileVisibility = profileVisibility;
        }
        if (profileImageUrl != null) {
            this.profileImageUrl = profileImageUrl;
        }
    }

    public void clearProfileImage() {
        this.profileImageUrl = null;
    }
}
