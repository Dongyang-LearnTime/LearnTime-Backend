package learntime.backend.domain.study.repository;

import learntime.backend.domain.study.model.StudyDailyPlan;
import learntime.backend.domain.studymember.model.StudyMember;
import learntime.backend.domain.study.model.StudyMemberContent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StudyUserContentRepository extends JpaRepository<StudyMemberContent, Long> {
    Optional<StudyMemberContent> findByStudyMemberAndStudyDailyPlan(StudyMember studyMember, StudyDailyPlan studyDailyPlan);
}
