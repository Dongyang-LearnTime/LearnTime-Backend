package learntime.backend.domain.user.model;

import jakarta.persistence.*;
import learntime.backend.domain.user.enums.Terms;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserTerms {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userTermsId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "terms_type", nullable = false)
    private Terms terms;

    @Column(nullable = false)
    private Boolean agreed; // 동의 여부

    @Column(nullable = false)
    private LocalDateTime agreedAt; // 동의 시점

    @Builder
    public UserTerms(Long userTermsId, User user, Terms terms, Boolean agreed, LocalDateTime agreedAt) {
        this.userTermsId = userTermsId;
        this.user = user;
        this.terms = terms;
        this.agreed = agreed;
        this.agreedAt = agreedAt;
    }

    public void updateAgreed(Boolean agreed) {
        this.agreed = agreed;
        this.agreedAt = LocalDateTime.now();
    }
}
