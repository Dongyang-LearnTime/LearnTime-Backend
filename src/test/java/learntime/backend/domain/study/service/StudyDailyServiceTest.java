package learntime.backend.domain.study.service;

import learntime.backend.domain.point.dto.PointEventDTO;
import learntime.backend.domain.study.dto.request.PlanCompleteRequestDTO;
import learntime.backend.domain.study.enums.CompletionStatus;
import learntime.backend.domain.study.enums.ProgressStatus;
import learntime.backend.domain.study.model.StudyDailyPlan;
import learntime.backend.domain.study.repository.StudyDailyPlanRepository;
import learntime.backend.domain.study.service.core.StudyDailyService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Constructor;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@Slf4j
@ExtendWith(MockitoExtension.class)
class StudyDailyServiceTest {

    @InjectMocks
    private StudyDailyService studyDailyService;

    @Mock
    private StudyDailyPlanRepository studyDailyPlanRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Test
    @DisplayName("일일 진도 완료 시: 성공 상태이면 이해도 점수가 반영된 포인트 이벤트가 발행된다.")
    // 일일 학습 성공 시 이해도 점수 반영된 보너스 포인트 이벤트 발행을 검증합니다.
    void completePlan_Success_WithBonusPoints() throws Exception {
        // given
        Long userId = 1L;
        Long planId = 10L;
        int understandingScore = 5;
        PlanCompleteRequestDTO request = new PlanCompleteRequestDTO(planId, CompletionStatus.SUCCESS, understandingScore);

        // Reflection을 이용한 엔티티 생성 (protected 생성자 우회)
        StudyDailyPlan studyDailyPlan = createInstance(StudyDailyPlan.class);
        ReflectionTestUtils.setField(studyDailyPlan, "progressStatus", ProgressStatus.IN_PROGRESS);

        given(studyDailyPlanRepository.findById(planId)).willReturn(Optional.of(studyDailyPlan));

        // when
        studyDailyService.completeStudyDailyPlan(request, userId);

        // then
        // 1. 엔티티 상태 검증
        assertThat(studyDailyPlan.getProgressStatus()).isEqualTo(ProgressStatus.COMPLETED);
        assertThat(studyDailyPlan.getCompletionStatus()).isEqualTo(CompletionStatus.SUCCESS);
        assertThat(studyDailyPlan.getCompletionDate()).isNotNull();

        // 2. 이벤트 발행 검증 (ArgumentCaptor 활용)
        ArgumentCaptor<PointEventDTO> eventCaptor = ArgumentCaptor.forClass(PointEventDTO.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());

        PointEventDTO event = eventCaptor.getValue();
        assertThat(event.userId()).isEqualTo(userId);
        // 기본 10p + (이해도 5 * 2p) = 20p
        assertThat(event.amount()).isEqualTo(20);
        assertThat(event.description()).contains("이해도: 5점");
        log.info("[테스트 결과] 성공 케이스 - 지급 포인트: {}p, 사유: {}", event.amount(), event.description());
    }

    @Test
    @DisplayName("일일 진도 완료 시: 실패 상태이면 최소 격려 포인트만 지급된다.")
    // 학습 실패 시 최소 포인트만 지급되는지 여부를 검증합니다.
    void completePlan_Failure_MinimalPoints() throws Exception {
        // given
        Long userId = 1L;
        Long planId = 11L;
        PlanCompleteRequestDTO request = new PlanCompleteRequestDTO(planId, CompletionStatus.FAILURE, 0);

        StudyDailyPlan studyDailyPlan = createInstance(StudyDailyPlan.class);
        ReflectionTestUtils.setField(studyDailyPlan, "progressStatus", ProgressStatus.IN_PROGRESS);

        given(studyDailyPlanRepository.findById(planId)).willReturn(Optional.of(studyDailyPlan));

        // when
        studyDailyService.completeStudyDailyPlan(request, userId);

        // then
        ArgumentCaptor<PointEventDTO> eventCaptor = ArgumentCaptor.forClass(PointEventDTO.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());

        PointEventDTO event = eventCaptor.getValue();
        assertThat(event.amount()).isEqualTo(2); // STUDY_COMPLETED_FAILURE (기본 2p 가정)
        assertThat(event.description()).doesNotContain("이해도");
        log.info("[테스트 결과] 실패 케이스 - 지급 포인트: {}p, 사유: {}", event.amount(), event.description());
    }

    // Reflection을 사용하여 protected 기본 생성자를 가진 클래스의 인스턴스를 생성합니다.
    private <T> T createInstance(Class<T> clazz) throws Exception {
        Constructor<T> constructor = clazz.getDeclaredConstructor();
        constructor.setAccessible(true);
        return constructor.newInstance();
    }
}
