package learntime.backend.domain.study_progress.repository;

import learntime.backend.domain.study.model.StudyDailyPlan;
import learntime.backend.domain.study_member.model.StudyMember;
import learntime.backend.domain.study_progress.model.StudyMemberContent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

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
