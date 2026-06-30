package learntime.backend.domain.study_plan.model;

import jakarta.persistence.*;
import learntime.backend.domain.study.model.Study;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.DayOfWeek;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "study_rest_day")
public class StudyRestDay {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long studyRestDayId;

    @Enumerated(EnumType.STRING)
    private DayOfWeek dayOfWeek; // MONDAY ~ SUNDAY

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "study_id", nullable = false)
    private Study study;

    @Builder
    public StudyRestDay(Study study, DayOfWeek dayOfWeek) {
        this.study = study;
        this.dayOfWeek = dayOfWeek;
    }

}
