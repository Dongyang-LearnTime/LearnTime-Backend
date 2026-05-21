package learntime.backend.domain.study.model;

import jakarta.persistence.*;
import learntime.backend.domain.study_member.enums.StudyPlanStatus;
import learntime.backend.domain.study_member.model.StudyMember;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


@Entity
@Table(name = "study")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class Study {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long studyId;

    @Column(nullable = false, length = 100)
    private String studyTitle;

    @Column(nullable = false)
    private String bookTitle;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StudyPlanStatus status = StudyPlanStatus.PLANNING;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    // 진도 맴버
    @OneToMany(mappedBy = "study", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<StudyMember> studyMembers = new ArrayList<>();

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
    public Study(String studyTitle, String bookTitle, LocalDate startDate, LocalDate endDate, StudyPlanStatus status) {
        this.studyTitle = studyTitle;
        this.bookTitle = bookTitle;
        this.startDate = startDate;
        this.endDate = endDate;
        this.status = (status == null) ? StudyPlanStatus.PLANNING : status;
    }

    // --- 비즈니스 로직 --- //
    public void updateStatus(StudyPlanStatus status) {
        this.status = status;
    }

    public void updateStudyInfo(String studyTitle, LocalDate startDate, LocalDate endDate) {
        this.studyTitle = studyTitle;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public void updateStudyDates(LocalDate startDate, LocalDate endDate) {
        this.startDate = startDate;
        this.endDate = endDate;
    }

}
