package learntime.backend.domain.study.model;

import jakarta.persistence.*;

import java.time.LocalDate;

@Entity
@Table(name = "study_rest_date")
public class StudyRestDate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long studyRestDateId;

    private LocalDate restDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "study_id")
    private Study study;
}