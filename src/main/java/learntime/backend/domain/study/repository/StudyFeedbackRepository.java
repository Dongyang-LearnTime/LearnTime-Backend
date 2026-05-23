package learntime.backend.domain.study.repository;

import learntime.backend.domain.study.model.StudyFeedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StudyFeedbackRepository extends JpaRepository<StudyFeedback, Long> {
    // 멤버의 피드백을 최신순으로 조회함
    List<StudyFeedback> findAllByStudyMember_StudyMemberIdOrderByCreatedAtDesc(Long studyMemberId);

    org.springframework.data.domain.Page<StudyFeedback> findAllByStudyMember_StudyMemberId(Long studyMemberId, org.springframework.data.domain.Pageable pageable);
}
