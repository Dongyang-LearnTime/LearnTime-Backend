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
}