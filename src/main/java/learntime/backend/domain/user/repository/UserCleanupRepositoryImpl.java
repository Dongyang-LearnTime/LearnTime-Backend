package learntime.backend.domain.user.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

public class UserCleanupRepositoryImpl implements UserCleanupRepository {
    @PersistenceContext
    private EntityManager entityManager;

    // Native SQL includes soft-deleted rows hidden by Hibernate restrictions.
    @Override
    @Transactional
    public int hardDeleteOldUsers(LocalDateTime thresholdDate) {
        entityManager.flush();
        List<?> ids = entityManager.createNativeQuery("""
                SELECT user_id FROM user
                WHERE deleted_at > '2000-01-01 00:00:00' AND deleted_at < :threshold
                ORDER BY user_id LIMIT 100 FOR UPDATE
                """).setParameter("threshold", thresholdDate).getResultList();
        if (ids.isEmpty()) {
            return 0;
        }

        String members = "SELECT study_member_id FROM study_member WHERE user_id IN (:ids)";
        String quizzes = "SELECT study_quiz_id FROM study_quiz WHERE study_member_id IN (" + members + ")";
        execute("DELETE FROM quiz_answer WHERE quiz_history_id IN (SELECT quiz_history_id FROM quiz_history WHERE study_quiz_id IN (" + quizzes + "))"
                + " OR quiz_question_id IN (SELECT quiz_question_id FROM quiz_question WHERE study_quiz_id IN (" + quizzes + "))", ids);
        execute("DELETE FROM quiz_history WHERE study_quiz_id IN (" + quizzes + ")", ids);
        execute("DELETE FROM quiz_question WHERE study_quiz_id IN (" + quizzes + ")", ids);
        execute("DELETE FROM study_quiz WHERE study_member_id IN (" + members + ")", ids);
        for (String table : List.of("study_notes", "study_feedback", "study_status", "study_member_content")) {
            execute("DELETE FROM " + table + " WHERE study_member_id IN (" + members + ")", ids);
        }
        execute("DELETE FROM study_join_request WHERE requester_user_id IN (:ids)", ids);
        execute("DELETE FROM study_invitation WHERE inviter_user_id IN (:ids) OR invited_user_id IN (:ids)", ids);
        execute("UPDATE study_forum_message SET study_member_id = NULL WHERE study_member_id IN (" + members + ")", ids);
        execute("DELETE FROM study_member WHERE user_id IN (:ids)", ids);

        // Preserve the other participant's inbox and shared discussion content.
        execute("UPDATE message SET completely_deleted_at = CASE WHEN receiver_deleted = true THEN COALESCE(completely_deleted_at, CURRENT_TIMESTAMP) ELSE completely_deleted_at END,"
                + " sender_deleted = true, sender_id = NULL WHERE sender_id IN (:ids)", ids);
        execute("UPDATE message SET completely_deleted_at = CASE WHEN sender_deleted = true THEN COALESCE(completely_deleted_at, CURRENT_TIMESTAMP) ELSE completely_deleted_at END,"
                + " receiver_deleted = true, receiver_id = NULL WHERE receiver_id IN (:ids)", ids);
        execute("UPDATE post SET user_id = NULL WHERE user_id IN (:ids)", ids);
        execute("UPDATE comment SET user_id = NULL WHERE user_id IN (:ids)", ids);

        execute("DELETE FROM exercise_parts WHERE exercise_record_id IN (SELECT exercise_record_id FROM exercise_record WHERE user_id IN (:ids))", ids);
        execute("DELETE FROM reminder WHERE calendar_record_id IN (SELECT calendar_record_id FROM calendar WHERE user_id IN (:ids))", ids);
        execute("DELETE FROM calendar WHERE user_id IN (:ids)", ids);
        execute("DELETE FROM routine_days WHERE routine_id IN (SELECT routine_id FROM routine WHERE user_id IN (:ids))", ids);
        execute("DELETE FROM friend WHERE user_id IN (:ids) OR friend_user_id IN (:ids)", ids);
        execute("DELETE FROM friend_request WHERE requester_id IN (:ids) OR receiver_id IN (:ids)", ids);
        execute("DELETE FROM user_block WHERE blocker_id IN (:ids) OR blocked_id IN (:ids)", ids);
        execute("DELETE FROM notification WHERE receiver_id IN (:ids)", ids);
        execute("UPDATE post p JOIN (SELECT post_id, COUNT(*) AS removed_count FROM post_like WHERE user_id IN (:ids) GROUP BY post_id) likes_to_remove ON p.post_id = likes_to_remove.post_id SET p.like_count = GREATEST(0, p.like_count - likes_to_remove.removed_count)", ids);
        for (String table : List.of("exercise_record", "weight_record", "meal", "routine",
                "point_history", "refresh_token", "prompt_quotas", "user_terms", "user_badge",
                "user_activity_stat", "profile", "post_like")) {
            execute("DELETE FROM " + table + " WHERE user_id IN (:ids)", ids);
        }
        int deleted = execute("DELETE FROM user WHERE user_id IN (:ids)", ids);
        entityManager.clear();
        return deleted;
    }

    private int execute(String sql, List<?> ids) {
        return entityManager.createNativeQuery(sql).setParameter("ids", ids).executeUpdate();
    }
}

