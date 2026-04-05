package learntime.backend.domain.study.service;

import jakarta.persistence.EntityManager;
import learntime.backend.domain.study.dto.request.*;
import learntime.backend.domain.study.dto.response.*;
import learntime.backend.domain.study.model.*;
import learntime.backend.domain.study.repository.*;
import learntime.backend.domain.user.model.User;
import learntime.backend.domain.user.repository.UserRepository;
import learntime.backend.global.dto.CustomUserDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional // 테스트 완료 후 트랜잭션 롤백 (메모리 및 DB 상태 복구)
class StudyCommandServiceTest {

    @Autowired
    private StudyCommandService studyCommandService;

    @Autowired
    private StudyRepository studyRepository;

    @Autowired
    private StudyDailyPlanRepository studyDailyPlanRepository;

    @Autowired
    private StudyRestDateRepository studyRestDateRepository;

    @Autowired
    private StudyRestDayRepository studyRestDayRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EntityManager em;

    // 매 테스트마다 재사용할 유저 픽스처 (Heap 재사용으로 객체 생성 비용 절감)
    private User testUser;

    @BeforeEach
    void setUp() {
        // [실무 최적화] MySQL 외래 키(FK) 무결성 체크 일시 해제 (네트워크 I/O 최적화 및 의존성 주입 최소화)
        // 불필요한 자식 테이블 Repository 주입 없이 즉각적인 삭제가 가능해집니다.
        em.createNativeQuery("SET FOREIGN_KEY_CHECKS = 0").executeUpdate();

        // 순서 상관없이 O(1) 시간 복잡도로 Batch Delete 실행
        studyDailyPlanRepository.deleteAllInBatch();
        studyRestDateRepository.deleteAllInBatch();
        studyRestDayRepository.deleteAllInBatch();
        studyRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();

        // [중요] 외래 키 체크 원상 복구 (DB 무결성 안전장치 재가동)
        em.createNativeQuery("SET FOREIGN_KEY_CHECKS = 1").executeUpdate();

        // given: 객체 생성 비용이 발생하는 유저 데이터 준비 및 DB Insert
        testUser = User.builder()
                .email("test@learntime.com")
                .password("password123!") // 로컬 가입 가정
                .name("테스트멘티")
                .socialProvider(User.AuthProvider.LOCAL)
                .role(User.Role.ROLE_USER)
                .build();

        userRepository.save(testUser);
    }

    @Test
    @DisplayName("학습 계획과 연관된 일차, 쉬는 날, 쉬는 요일이 모두 정상적으로 DB에 저장된다.")
    void saveStudyPlan_success() {
        // given: API Request DTO 및 Gemini Response DTO 모킹
        GeminiStudyRequestDTO request = new GeminiStudyRequestDTO(
                "토비의 스프링",
                "스프링 부트 마스터",
                "http://yes24.com/...",
                LocalDate.of(2026, 4, 1),
                LocalDate.of(2026, 4, 10),
                List.of(DayOfWeek.SUNDAY),
                List.of(LocalDate.of(2026, 4, 5))
        );

        StudyPlanResponseDTO.DailyPlan dailyPlanDto1 = new StudyPlanResponseDTO.DailyPlan(1, "1장 읽기");
        StudyPlanResponseDTO.DailyPlan dailyPlanDto2 = new StudyPlanResponseDTO.DailyPlan(2, "2장 읽기");
        StudyPlanResponseDTO geminiResult = new StudyPlanResponseDTO(List.of(dailyPlanDto1, dailyPlanDto2));

        // CustomUserDetails 모킹 (Stack 메모리 값 복사 전달로 RESTful 계층 간 결합도 감소)
        CustomUserDetails userDetails = new CustomUserDetails(
                testUser.getUserId(),
                "test@email.com",
                "ROLE_USER",
                true
        );

        // when: 서비스 로직 실행
        studyCommandService.saveStudyPlan(request, geminiResult, userDetails.userId());

        // 강제로 Insert 쿼리를 DB에 동기화시키고, 1차 캐시를 비워 실제 DB 제약조건 검증
        em.flush();
        em.clear();

        // then: 연관 데이터베이스 저장 검증
        List<Study> studies = studyRepository.findAll();
        assertThat(studies).hasSize(1);

        Study savedStudy = studies.getFirst();
        assertThat(savedStudy.getStudyTitle()).isEqualTo("스프링 부트 마스터");
        assertThat(savedStudy.getUser().getUserId()).isEqualTo(testUser.getUserId());

        assertThat(studyDailyPlanRepository.findAll()).hasSize(2);
        assertThat(studyRestDateRepository.findAll()).hasSize(1);
        assertThat(studyRestDayRepository.findAll()).hasSize(1);
    }

    @Test
    @DisplayName("부모 엔티티(Study) 삭제 시 CascadeType.ALL에 의해 연관된 자식 엔티티가 모두 삭제된다.")
    void cascade_delete_test() {
        // given: 사전 데이터 저장
        GeminiStudyRequestDTO request = new GeminiStudyRequestDTO(
                "토비의 스프링",
                "스프링 부트 마스터",
                "http://yes24.com/...",
                LocalDate.of(2026, 4, 1),
                LocalDate.of(2026, 4, 10),
                List.of(DayOfWeek.SUNDAY),
                List.of(LocalDate.of(2026, 4, 5))
        );

        StudyPlanResponseDTO.DailyPlan dailyPlanDto = new StudyPlanResponseDTO.DailyPlan(1, "테스트 내용");
        StudyPlanResponseDTO geminiResult = new StudyPlanResponseDTO(List.of(dailyPlanDto));

        studyCommandService.saveStudyPlan(request, geminiResult, testUser.getUserId());

        em.flush();
        em.clear();

        Study savedStudy = studyRepository.findAll().getFirst();
        assertThat(studyDailyPlanRepository.findAll()).isNotEmpty();

        // when: 부모 엔티티 단일 삭제 (JPA 영속성 컨텍스트를 통한 일반 삭제 -> Cascade 정상 동작)
        studyRepository.delete(savedStudy);

        // Delete 쿼리 발생 확인 및 영속성 컨텍스트 초기화
        em.flush();
        em.clear();

        // then: Cascade 제약조건에 의해 자식 데이터 물리적 삭제 검증
        assertThat(studyRepository.findAll()).isEmpty();
        assertThat(studyDailyPlanRepository.findAll()).isEmpty();
        assertThat(studyRestDateRepository.findAll()).isEmpty();
        assertThat(studyRestDayRepository.findAll()).isEmpty();
    }

    @Test
    @DisplayName("AI 진도 재설정 시, 완료된 일정은 유지하고 미완료 일정은 삭제 후 새 일정으로 덮어쓴다.")
    void replanStudy_success() {
        // given 1: 기존 스터디 일정 데이터 더미 생성
        Study study = Study.builder()
                .studyTitle("기존 스터디 제목")
                .bookTitle("스프링 부트 마스터")
                .startDate(LocalDate.of(2026, 4, 1))
                .endDate(LocalDate.of(2026, 4, 10))
                .user(testUser) // 유저 매핑
                .build();
        studyRepository.save(study);
        Long userId = testUser.getUserId();

        studyRestDayRepository.save(StudyRestDay.builder().study(study).dayOfWeek(DayOfWeek.SUNDAY).build());
        studyRestDateRepository.save(StudyRestDate.builder().study(study).restDate(LocalDate.of(2026, 4, 5)).build());

        StudyDailyPlan completed1 = StudyDailyPlan.builder().study(study).dayNumber(1).planContent("1장 읽기").build();
        ReflectionTestUtils.setField(completed1, "progressStatus", StudyDailyPlan.ProgressStatus.COMPLETED);

        StudyDailyPlan completed2 = StudyDailyPlan.builder().study(study).dayNumber(2).planContent("2장 읽기").build();
        ReflectionTestUtils.setField(completed2, "progressStatus", StudyDailyPlan.ProgressStatus.COMPLETED);

        StudyDailyPlan inProgress = StudyDailyPlan.builder().study(study).dayNumber(3).planContent("3장 읽기").build();

        studyDailyPlanRepository.saveAll(List.of(completed1, completed2, inProgress));

        em.flush();
        em.clear();

        // given 2: 변경할 스케줄 정보
        GeminiReplanRequestDTO request = new GeminiReplanRequestDTO(
                "수정된 스터디 제목",
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 10),
                List.of(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY),
                List.of(LocalDate.of(2026, 5, 5))
        );

        // given 3: gemini로 재생성한 일정 모킹
        StudyPlanResponseDTO.DailyPlan newPlan1 = new StudyPlanResponseDTO.DailyPlan(1, "3장 전반부 읽기");
        StudyPlanResponseDTO.DailyPlan newPlan2 = new StudyPlanResponseDTO.DailyPlan(2, "3장 후반부 읽기");
        StudyPlanResponseDTO geminiResult = new StudyPlanResponseDTO(List.of(newPlan1, newPlan2));

        // when: 재설정 로직 실행
        studyCommandService.replanStudy(study.getStudyId(), request, geminiResult, userId); //

        em.flush();
        em.clear();

        // then 4: DB 저장 및 덮어쓰기 상태 점검
        Study updatedStudy = studyRepository.findAll().getFirst();
        assertThat(updatedStudy.getStudyTitle()).isEqualTo("수정된 스터디 제목");

        assertThat(studyRestDayRepository.findAll()).hasSize(2)
                .extracting("dayOfWeek").containsExactlyInAnyOrder(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY);
        assertThat(studyRestDateRepository.findAll()).hasSize(1)
                .extracting("restDate").containsExactly(LocalDate.of(2026, 5, 5));

        List<StudyDailyPlan> finalPlans = studyDailyPlanRepository.findAll();
        assertThat(finalPlans).hasSize(4);
        assertThat(finalPlans).extracting("dayNumber")
                .containsExactlyInAnyOrder(1, 2, 3, 4);
        assertThat(finalPlans).extracting("planContent")
                .contains("1장 읽기", "2장 읽기", "3장 전반부 읽기", "3장 후반부 읽기");
    }
}