package learntime.backend.domain.study.model;

import jakarta.persistence.*;
import learntime.backend.domain.user.model.User;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


// 향후 프런트에서 로그인, 회원가입 기능 구현 후, user 테이블과 1대N 연결
@Entity
@Table(name = "study", indexes = {
        @Index(name = "idx_study_user_id", columnList = "user_id") // Soft Delete 벌크 연산 및 조회 성능 최적화
})
@SQLDelete(sql = "UPDATE study SET is_deleted = true WHERE study_id = ?")
@SQLRestriction("is_deleted = false")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class Study {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // MySQL Auto Increment
    private Long studyId;

    @Column(nullable = false, length = 100)
    private String studyTitle;

    @Column(nullable = false)
    private String bookTitle;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate endDate;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "is_deleted", nullable = false)
    private boolean isDeleted = false;

    // 일차별 진도 내용
    @OneToMany(mappedBy = "study", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<StudyDailyPlan> studyDailyPlans = new ArrayList<>();

    // 쉬는 요일
    @OneToMany(mappedBy = "study", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<StudyRestDay> restDays = new ArrayList<>();

    // 쉬는 날
    @OneToMany(mappedBy = "study", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<StudyRestDate> restDates = new ArrayList<>();

    @Builder
    public Study(String studyTitle, String bookTitle, LocalDate startDate, LocalDate endDate, User user) {
        this.studyTitle = studyTitle;
        this.bookTitle = bookTitle;
        this.startDate = startDate;
        this.endDate = endDate;
        this.user = user;
    }

    // --- 비즈니스 로직 --- //
    public void updateStudyInfo(String studyTitle, LocalDate startDate, LocalDate endDate) {
        this.studyTitle = studyTitle;
        this.startDate = startDate;
        this.endDate = endDate;
    }
}