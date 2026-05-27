package learntime.backend.domain.study.service.core;

import learntime.backend.domain.study.dto.request.UpdateStudyRestScheduleRequestDTO;
import learntime.backend.domain.study.error.code.StudyErrorCode;
import learntime.backend.domain.study.error.exception.StudyException;
import learntime.backend.domain.study.model.Study;
import learntime.backend.domain.study.model.StudyDailyPlan;
import learntime.backend.domain.study.repository.StudyDailyPlanRepository;
import learntime.backend.domain.study.repository.StudyRepository;
import learntime.backend.domain.study.repository.StudyRestDateRepository;
import learntime.backend.domain.study.repository.StudyRestDayRepository;
import learntime.backend.domain.study.service.util.StudyDateCalculator;
import learntime.backend.domain.study_member.enums.StudyMemberRole;
import learntime.backend.domain.study_member.enums.StudyMemberStatus;
import learntime.backend.domain.study_member.enums.StudyPlanStatus;
import learntime.backend.domain.study_member.model.StudyMember;
import learntime.backend.domain.study_member.repository.StudyMemberRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StudyRestServiceTest {

    @Mock
    private StudyRepository studyRepository;

    @Mock
    private StudyDailyPlanRepository studyDailyPlanRepository;

    @Mock
    private StudyMemberRepository studyMemberRepository;

    @Mock
    private StudyRestDateRepository studyRestDateRepository;

    @Mock
    private StudyRestDayRepository studyRestDayRepository;

    @Mock
    private StudyRestManager studyRestManager;

    @Mock
    private StudyDateCalculator studyDateCalculator;

    @InjectMocks
    private StudyRestService studyRestService;

    @Test
    @DisplayName("휴무 재조정 실패 - 오늘을 새 휴무일로 변경할 수 없음")
    void updateRestSchedule_TodayBecomesRest() {
        Long studyId = 1L;
        Long userId = 1L;
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));
        Study study = Study.builder()
                .studyTitle("study")
                .bookTitle("book")
                .startDate(today.minusDays(1))
                .endDate(today.plusDays(10))
                .status(StudyPlanStatus.READY)
                .build();
        StudyMember studyMember = mockOwnerMember();

        given(studyRepository.findById(studyId)).willReturn(Optional.of(study));
        given(studyMemberRepository.findByStudy_StudyIdAndUser_UserIdAndStatus(studyId, userId, StudyMemberStatus.ACTIVE))
                .willReturn(Optional.of(studyMember));
        given(studyRestDayRepository.findAllByStudy_StudyId(studyId)).willReturn(List.of());
        given(studyRestDateRepository.findAllByStudy_StudyId(studyId)).willReturn(List.of());

        UpdateStudyRestScheduleRequestDTO request = new UpdateStudyRestScheduleRequestDTO(
                List.of(today.getDayOfWeek()),
                List.of()
        );

        assertThatThrownBy(() -> studyRestService.updateRestSchedule(studyId, request, userId))
                .isInstanceOf(StudyException.class)
                .hasMessageContaining(StudyErrorCode.TODAY_REST_CHANGE_NOT_ALLOWED.getMessage());

        verify(studyDailyPlanRepository, never()).findAllByStudy_StudyIdOrderByDayNumberAsc(anyLong());
        verify(studyRestDayRepository, never()).deleteAllByStudy_StudyId(anyLong());
    }

    @Test
    @DisplayName("휴무 재조정 성공 - 공부 내용은 유지하고 미래 날짜만 변경")
    void updateRestSchedule_ReassignFuturePlanDatesOnly() {
        Long studyId = 1L;
        Long userId = 1L;
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));
        Study study = Study.builder()
                .studyTitle("study")
                .bookTitle("book")
                .startDate(today.minusDays(1))
                .endDate(today.plusDays(3))
                .status(StudyPlanStatus.READY)
                .build();
        StudyMember studyMember = mockOwnerMember();

        StudyDailyPlan yesterdayPlan = StudyDailyPlan.builder()
                .dayNumber(1)
                .planDate(today.minusDays(1))
                .planContent("past")
                .study(study)
                .build();
        StudyDailyPlan todayPlan = StudyDailyPlan.builder()
                .dayNumber(2)
                .planDate(today)
                .planContent("today")
                .study(study)
                .build();
        StudyDailyPlan futurePlan1 = StudyDailyPlan.builder()
                .dayNumber(3)
                .planDate(today.plusDays(1))
                .planContent("future-1")
                .study(study)
                .build();
        StudyDailyPlan futurePlan2 = StudyDailyPlan.builder()
                .dayNumber(4)
                .planDate(today.plusDays(2))
                .planContent("future-2")
                .study(study)
                .build();
        List<StudyDailyPlan> plans = List.of(yesterdayPlan, todayPlan, futurePlan1, futurePlan2);
        List<LocalDate> recalculatedDates = List.of(today.plusDays(2), today.plusDays(4));
        UpdateStudyRestScheduleRequestDTO request = new UpdateStudyRestScheduleRequestDTO(
                List.of(today.plusDays(1).getDayOfWeek()),
                List.of(today.plusDays(3))
        );

        given(studyRepository.findById(studyId)).willReturn(Optional.of(study));
        given(studyMemberRepository.findByStudy_StudyIdAndUser_UserIdAndStatus(studyId, userId, StudyMemberStatus.ACTIVE))
                .willReturn(Optional.of(studyMember));
        given(studyRestDayRepository.findAllByStudy_StudyId(studyId)).willReturn(List.of());
        given(studyRestDateRepository.findAllByStudy_StudyId(studyId)).willReturn(List.of());
        given(studyDailyPlanRepository.findAllByStudy_StudyIdOrderByDayNumberAsc(studyId)).willReturn(plans);
        given(studyDateCalculator.buildPlanDates(
                today.plusDays(1),
                2,
                Set.of(today.plusDays(1).getDayOfWeek()),
                Set.of(today.plusDays(3))
        )).willReturn(recalculatedDates);

        studyRestService.updateRestSchedule(studyId, request, userId);

        verify(studyRestDayRepository).deleteAllByStudy_StudyId(studyId);
        verify(studyRestDateRepository).deleteAllByStudy_StudyId(studyId);
        verify(studyRestManager).saveRestDays(study, request.restDays());
        verify(studyRestManager).saveRestDates(study, request.restDates());

        org.assertj.core.api.Assertions.assertThat(yesterdayPlan.getPlanDate()).isEqualTo(today.minusDays(1));
        org.assertj.core.api.Assertions.assertThat(todayPlan.getPlanDate()).isEqualTo(today);
        org.assertj.core.api.Assertions.assertThat(futurePlan1.getPlanDate()).isEqualTo(today.plusDays(2));
        org.assertj.core.api.Assertions.assertThat(futurePlan2.getPlanDate()).isEqualTo(today.plusDays(4));
        org.assertj.core.api.Assertions.assertThat(futurePlan1.getPlanContent()).isEqualTo("future-1");
        org.assertj.core.api.Assertions.assertThat(futurePlan2.getPlanContent()).isEqualTo("future-2");
        org.assertj.core.api.Assertions.assertThat(study.getEndDate()).isEqualTo(today.plusDays(4));
    }

    private StudyMember mockOwnerMember() {
        StudyMember studyMember = mock(StudyMember.class);
        given(studyMember.isActive()).willReturn(true);
        given(studyMember.getStudyMemberRole()).willReturn(StudyMemberRole.OWNER);
        return studyMember;
    }
}
