package learntime.backend.domain.study.repository;

import learntime.backend.domain.study.model.StudyFeedback;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StudyFeedbackRepository extends JpaRepository<StudyFeedback, Long> {
    // 멤버의 피드백을 최신순으로 조회함
    List<StudyFeedback> findAllByStudyMember_StudyMemberIdOrderByCreatedAtDesc(Long studyMemberId);

    Page<StudyFeedback> findAllByStudyMember_StudyMemberId(Long studyMemberId, Pageable pageable);

    @Query("SELECT f FROM StudyFeedback f " +
           "JOIN FETCH f.studyMember sm " +
           "JOIN FETCH sm.study s " +
           "WHERE sm.user.userId = :userId " +
           "ORDER BY f.createdAt DESC")
    List<StudyFeedback> findTop3ByUserId(@Param("userId") Long userId, Pageable pageable);
}
