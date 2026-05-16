package learntime.backend.domain.study.repository;

import learntime.backend.domain.study.model.StudyMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StudyMemberRepository extends JpaRepository<StudyMember, Long> {
}
