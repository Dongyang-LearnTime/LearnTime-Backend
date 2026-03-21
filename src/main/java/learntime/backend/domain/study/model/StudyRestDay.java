package learntime.backend.domain.study.model;

import jakarta.persistence.*;

import java.time.DayOfWeek;

@Entity
@Table(name = "study_rest_day")
public class StudyRestDay {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long studyRestDayId;

    @Enumerated(EnumType.STRING)
    private DayOfWeek dayOfWeek; // MONDAY ~ SUNDAY

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "study_id")
    private Study study;
}
