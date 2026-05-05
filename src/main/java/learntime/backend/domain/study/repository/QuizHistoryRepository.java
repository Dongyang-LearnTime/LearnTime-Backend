package learntime.backend.domain.study.repository;

import learntime.backend.domain.study.model.QuizHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuizHistoryRepository extends JpaRepository<QuizHistory, Long> {
}
