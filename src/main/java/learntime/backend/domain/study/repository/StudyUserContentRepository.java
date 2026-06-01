package learntime.backend.domain.study.repository;

import learntime.backend.domain.study.model.StudyDailyPlan;
import learntime.backend.domain.study_member.model.StudyMember;
import learntime.backend.domain.study.model.StudyMemberContent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface StudyUserContentRepository extends JpaRepository<StudyMemberContent, Long> {
    Optional<StudyMemberContent> findByStudyMemberAndStudyDailyPlan(StudyMember studyMember, StudyDailyPlan studyDailyPlan);

    List<StudyMemberContent> findAllByStudyDailyPlanAndStudyMember(
            StudyDailyPlan studyDailyPlan,
            StudyMember studyMember
    );

}
