package learntime.backend.domain.study.repository;

import learntime.backend.domain.study.model.Study;
import learntime.backend.domain.study.model.StudyDailyPlan;
import learntime.backend.domain.study.model.StudyDailyPlan.CompletionStatus;
import learntime.backend.domain.user.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest; // [변경] 가장 안정적인 기본 테스트 패키지
import org.springframework.transaction.annotation.Transactional; // [추가] 데이터 롤백 보장

import jakarta.persistence.EntityManager;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest // 프로젝트의 전체 설정을 로드하여 환경 차이로 인한 오류 방지
@Transactional  // 테스트 실행 후 DB 상태를 자동으로 Rollback하여 격리성 유지
class StudyDailyPlanRepositoryTest {

    @Autowired
    private StudyDailyPlanRepository studyDailyPlanRepository;

    @Autowired
    private EntityManager entityManager;

    private Study testStudy;
    private LocalDate today;

    @BeforeEach
    void setUp() {
        today = LocalDate.now();

        // 1. User 생성 (정의하신 @Builder 생성자 활용)
        User testUser = User.builder()
                .email("test@test.com")
                .password("password123")
                .name("테스터")
                .role(User.Role.ROLE_USER) // null 입력 시 생성자 로직에 의해 기본값 할당됨
                .build();

        entityManager.persist(testUser); // 부모 엔티티 우선 영속화

        testStudy = Study.builder()
                .studyTitle("스프링 실무 스터디")
                .bookTitle("Spring Boot 4.0")
                .startDate(today.minusDays(5))
                .endDate(today.plusDays(10))
                .user(testUser) // 영속 상태의 User 주입 (FK 제약조건 충족)
                .build();

        entityManager.persist(testStudy);
    }
    @Test
    @DisplayName("오늘 이전의 미완료 계획을 벌크 연산으로 실패 처리한다")
    void bulkFailIncompletePlans_success() {
        // Given
        LocalDate yesterday = today.minusDays(1);

        // 1. 타겟: 어제 날짜 + 미시작
        StudyDailyPlan plan1 = StudyDailyPlan.builder()
                .study(testStudy)
                .dayNumber(1)
                .planDate(yesterday)
                .planContent("어제 계획 1")
                .build();

        // 2. 비타겟: 오늘 날짜 + 미시작
        StudyDailyPlan plan2 = StudyDailyPlan.builder()
                .study(testStudy)
                .dayNumber(2)
                .planDate(today)
                .planContent("오늘 계획")
                .build();

        studyDailyPlanRepository.save(plan1);
        studyDailyPlanRepository.save(plan2);

        entityManager.flush();
        entityManager.clear();

        // When
        int updatedCount = studyDailyPlanRepository.bulkFailIncompletePlans(today);

        // Then
        assertThat(updatedCount).isGreaterThanOrEqualTo(1);

        StudyDailyPlan updatedPlan1 = studyDailyPlanRepository.findById(plan1.getStudyDailyPlanId()).get();
        assertThat(updatedPlan1.getCompletionStatus()).isEqualTo(CompletionStatus.FAILURE);
    }
}