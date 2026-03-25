package learntime.backend.domain.study.service;

import jakarta.persistence.EntityManager;
import learntime.backend.domain.study.dto.request.*;
import learntime.backend.domain.study.dto.response.*;
import learntime.backend.domain.study.model.*;
import learntime.backend.domain.study.repository.*;
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
@Transactional // 테스트 완료 후 트랜잭션 롤백 (테스트가 생성한 데이터 한정)
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
    private EntityManager em;

    @BeforeEach
    void setUp() {
        studyDailyPlanRepository.deleteAllInBatch();
        studyRestDateRepository.deleteAllInBatch();
        studyRestDayRepository.deleteAllInBatch();
        studyRepository.deleteAllInBatch();
    }

    @Test
    @DisplayName("학습 계획과 연관된 일차, 쉬는 날, 쉬는 요일이 모두 정상적으로 DB에 저장된다.")
    void saveStudyPlan_success() {
        // given: 객체 생성 비용이 발생하는 테스트 데이터 준비
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

        // when: 서비스 로직 실행
        studyCommandService.saveStudyPlan(request, geminiResult);

        // 강제로 Insert 쿼리를 DB에 동기화시키고(네트워크 I/O 발생),
        // 1차 캐시(Heap)를 비워 실제 DB 저장 여부 검증 준비
        em.flush();
        em.clear();

        // then: 연관 데이터베이스 저장 검증
        List<Study> studies = studyRepository.findAll();

        assertThat(studies).hasSize(1);

        Study savedStudy = studies.getFirst();
        assertThat(savedStudy.getStudyTitle()).isEqualTo("스프링 부트 마스터");

        // 자식 데이터 삽입 검증
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

        studyCommandService.saveStudyPlan(request, geminiResult);

        // Insert 쿼리 반영 후 1차 캐시 초기화
        em.flush();
        em.clear();

        // 저장된 부모 엔티티 조회
        Study savedStudy = studyRepository.findAll().getFirst();

        // 자식 데이터가 존재하는지 사전 검증
        assertThat(studyDailyPlanRepository.findAll()).isNotEmpty();

        // when: 부모 엔티티 단일 삭제
        studyRepository.delete(savedStudy);

        // Delete 쿼리를 강제로 발생시켜 DB 계층에서 Cascade 동작 확인
        em.flush();
        em.clear();

        // then: Cascade 제약조건에 의해 자식 데이터들도 물리적으로 삭제되었는지 검증
        assertThat(studyRepository.findAll()).isEmpty();
        assertThat(studyDailyPlanRepository.findAll()).isEmpty();
        assertThat(studyRestDateRepository.findAll()).isEmpty();
        assertThat(studyRestDayRepository.findAll()).isEmpty();
    }

    @Test
    @DisplayName("AI 진도 재설정 시, 완료된 일정은 유지하고 미완료 일정은 삭제 후 새 일정으로 덮어쓴다.")
    void replanStudy_success() {
        // given 1: 기존 남은 스터디 일정 데이터는 더미데이터로 생성 (완료된거 2개, 안된거 1개 섞어서)
        Study study = Study.builder()
                .studyTitle("기존 스터디 제목")
                .bookTitle("스프링 부트 마스터")
                .startDate(LocalDate.of(2026, 4, 1))
                .endDate(LocalDate.of(2026, 4, 10))
                .build();
        studyRepository.save(study);

        // 기존 스케줄 (쉬는 요일: 일요일, 쉬는 날: 4월 5일)
        studyRestDayRepository.save(StudyRestDay.builder().study(study).dayOfWeek(DayOfWeek.SUNDAY).build());
        studyRestDateRepository.save(StudyRestDate.builder().study(study).restDate(LocalDate.of(2026, 4, 5)).build());

        // 완료된 진도 더미데이터
        StudyDailyPlan completed1 = StudyDailyPlan.builder().study(study).dayNumber(1).planContent("1장 읽기").build();
        ReflectionTestUtils.setField(completed1, "progressStatus", StudyDailyPlan.ProgressStatus.COMPLETED);

        StudyDailyPlan completed2 = StudyDailyPlan.builder().study(study).dayNumber(2).planContent("2장 읽기").build();
        ReflectionTestUtils.setField(completed2, "progressStatus", StudyDailyPlan.ProgressStatus.COMPLETED);

        // 미완료(진행 전/중) 진도 더미데이터
        StudyDailyPlan inProgress = StudyDailyPlan.builder().study(study).dayNumber(3).planContent("3장 읽기").build();
        
        studyDailyPlanRepository.saveAll(List.of(completed1, completed2, inProgress));

        em.flush();
        em.clear();

        // 기존 진도 로그 출력
        System.out.println("\n=== [LOG] 기존 진도 ===");
        studyDailyPlanRepository.findAll().forEach(plan ->
                System.out.println(plan.getDayNumber() + "일차 [" + plan.getProgressStatus() + "]: " + plan.getPlanContent())
        );

        // given 2: 변경할 스케줄 정보 (쉬는 날, 쉬는 요일 수정)
        GeminiReplanRequestDTO request = new GeminiReplanRequestDTO(
                "수정된 스터디 제목",
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 10),
                List.of(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY), // 토, 일 쉬는 요일로 변경
                List.of(LocalDate.of(2026, 5, 5))              // 쉬는 날 변경
        );

        // given 3: gemini로 재생성한 일정 모킹 (미완료된 3장을 남은 기간에 맞춰 분배했다고 가정)
        StudyPlanResponseDTO.DailyPlan newPlan1 = new StudyPlanResponseDTO.DailyPlan(1, "3장 전반부 읽기");
        StudyPlanResponseDTO.DailyPlan newPlan2 = new StudyPlanResponseDTO.DailyPlan(2, "3장 후반부 읽기");
        StudyPlanResponseDTO geminiResult = new StudyPlanResponseDTO(List.of(newPlan1, newPlan2));

        // when: 재설정 로직 실행
        studyCommandService.replanStudy(study.getStudyId(), request, geminiResult);

        em.flush();
        em.clear();

        // 재설정된 진도 로그 출력
        System.out.println("\n=== [LOG] 재설정된 진도 ===");
        studyDailyPlanRepository.findAll().forEach(plan ->
                System.out.println(plan.getDayNumber() + "일차 [" + plan.getProgressStatus() + "]: " + plan.getPlanContent())
        );

        // then 4: DB 저장 및 덮어쓰기 검증
        Study updatedStudy = studyRepository.findAll().getFirst();
        assertThat(updatedStudy.getStudyTitle()).isEqualTo("수정된 스터디 제목");
        
        // 쉬는 날, 쉬는 요일 기존 더미데이터에서 수정되었는지 검증
        assertThat(studyRestDayRepository.findAll()).hasSize(2)
                .extracting("dayOfWeek").containsExactlyInAnyOrder(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY);
        assertThat(studyRestDateRepository.findAll()).hasSize(1)
                .extracting("restDate").containsExactly(LocalDate.of(2026, 5, 5));

        // 진도 검증: 미완료였던 "3장 읽기"가 지워지고, 완료된 1,2장 뒤에 새 일정이 3, 4일차로 붙어야 함
        List<StudyDailyPlan> finalPlans = studyDailyPlanRepository.findAll();
        assertThat(finalPlans).hasSize(4); // 완료 2개 + 신규 2개
        assertThat(finalPlans).extracting("dayNumber")
                .containsExactlyInAnyOrder(1, 2, 3, 4); 
        assertThat(finalPlans).extracting("planContent")
                .contains("1장 읽기", "2장 읽기", "3장 전반부 읽기", "3장 후반부 읽기");
    }
}