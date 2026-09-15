package learntime.backend.domain.study_forum.repository;

import learntime.backend.domain.study_forum.model.StudyForumMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface StudyForumMessageRepository extends JpaRepository<StudyForumMessage, Long> {
    @Query("""
            SELECT m FROM StudyForumMessage m LEFT JOIN FETCH m.author member LEFT JOIN FETCH member.user a
            WHERE m.study.studyId = :studyId
              AND (:beforeId IS NULL OR m.messageId < :beforeId)
              AND NOT EXISTS (
                SELECT b FROM UserBlock b
                WHERE (b.blocker.userId = :userId AND b.blocked.userId = a.userId)
                   OR (b.blocked.userId = :userId AND b.blocker.userId = a.userId)
              )
            ORDER BY m.messageId DESC
            """)
    List<StudyForumMessage> findMessages(@Param("studyId") Long studyId, @Param("userId") Long userId,
                                         @Param("beforeId") Long beforeId, Pageable pageable);

    Optional<StudyForumMessage> findByMessageIdAndStudy_StudyId(Long messageId, Long studyId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE StudyForumMessage m SET m.author = null WHERE m.author.user.userId = :userId")
    void detachAuthorByUserId(@Param("userId") Long userId);
}

