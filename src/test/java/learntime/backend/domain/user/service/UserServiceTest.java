package learntime.backend.domain.user.service;

import jakarta.persistence.EntityManager;
import learntime.backend.domain.exercise.model.WeightRecord;
import learntime.backend.domain.study.model.Study;
import learntime.backend.domain.user.enums.Role;
import learntime.backend.domain.user.model.User;
import learntime.backend.domain.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class UserServiceTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EntityManager em;

    @Test
    @DisplayName("User 삭제 시 연쇄 삭제 동작 검증 (Study 포함)")
    void cascadeDeleteTest() {
        // ================== given ==================
        User user = User.builder()
                .email("test@test.com")
                .password("1234")
                .name("tester")
                .role(Role.ROLE_USER)
                .build();

        // WeightRecord
        WeightRecord weight = new WeightRecord(user.getUserId(), user, 100.1, 22.2);
        user.getWeightRecord().add(weight);

        // Study 추가
        Study study = new Study(
                "일정",
                "JPA 공부",
                LocalDate.parse("2026-12-12"), // 시작일
                LocalDate.parse("2027-01-22"), // 종료일
                user
        );

        user.getStudy().add(study);

        userRepository.save(user);
        em.flush();
        em.clear();

        Long userId = user.getUserId();

        // ================== when ==================
        User foundUser = userRepository.findById(userId).orElseThrow();

        userRepository.delete(foundUser);
        em.flush();
        em.clear();

        // ================== then ==================

        // 1. User soft delete 확인
        User deletedUser = em.find(User.class, userId);
        assertThat(deletedUser).isNull();

        // 2. WeightRecord 삭제 확인
        Long weightCount = em.createQuery(
                        "SELECT COUNT(w) FROM WeightRecord w", Long.class)
                .getSingleResult();

        assertThat(weightCount).isEqualTo(0L);

        // 3. Study 확인
        Long studyCount = em.createQuery(
                        "SELECT COUNT(s) FROM Study s", Long.class)
                .getSingleResult();

        assertThat(studyCount).isEqualTo(1L);
    }
}