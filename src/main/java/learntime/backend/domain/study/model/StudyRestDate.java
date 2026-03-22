package learntime.backend.domain.study.model;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "study_rest_date")
public class StudyRestDate {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long studyRestDateId;

    private LocalDate restDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "study_id", nullable = false)
    private Study study;

    @Builder
    public StudyRestDate(Study study, LocalDate restDate) {
        this.study = study;
        this.restDate = restDate;
    }
}