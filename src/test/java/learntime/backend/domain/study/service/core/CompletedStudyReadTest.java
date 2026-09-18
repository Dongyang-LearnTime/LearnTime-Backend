package learntime.backend.domain.study.service.core;

import learntime.backend.domain.study.error.exception.StudyException;
import learntime.backend.domain.study.model.Study;
import learntime.backend.domain.user.model.User;
import learntime.backend.domain.study.repository.StudyDailyPlanRepository;
import learntime.backend.domain.study.repository.StudyRepository;
import learntime.backend.domain.study_member.enums.StudyMemberStatus;
import learntime.backend.domain.study_member.model.StudyMember;
import learntime.backend.domain.study_member.repository.StudyMemberRepository;
import learntime.backend.domain.study_plan.repository.StudyRestDateRepository;
import learntime.backend.domain.study_plan.repository.StudyRestDayRepository;
import learntime.backend.domain.study_progress.repository.StudyStatusRepository;
import learntime.backend.domain.study_progress.repository.StudyUserContentRepository;
import learntime.backend.domain.study_progress.scheduler.StudyDailyPlanScheduler;
import learntime.backend.domain.study_progress.service.StudyDailyService;
import learntime.backend.domain.study_progress.service.StudyQueryService;
import learntime.backend.domain.study_progress.service.StudyUserContentService;
import learntime.backend.domain.quiz.repository.QuizHistoryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CompletedStudyReadTest {
    @Mock StudyRepository studyRepository;
    @Mock StudyDailyPlanRepository studyDailyPlanRepository;
    @Mock StudyMemberRepository studyMemberRepository;
    @Mock StudyRestDateRepository studyRestDateRepository;
    @Mock StudyRestDayRepository studyRestDayRepository;
    @Mock StudyStatusRepository studyStatusRepository;
    @Mock StudyUserContentRepository studyUserContentRepository;
    @Mock QuizHistoryRepository quizHistoryRepository;
    @InjectMocks StudyDailyService dailyService;
    @InjectMocks StudyUserContentService contentService;
    @InjectMocks StudyQueryService queryService;

    private static final List<StudyMemberStatus> READ_STATUSES = List.of(
            StudyMemberStatus.ACTIVE, StudyMemberStatus.WITHDRAWN, StudyMemberStatus.COMPLETED);
    private static final List<StudyMemberStatus> CURRENT_STATUSES = List.of(
            StudyMemberStatus.ACTIVE, StudyMemberStatus.COMPLETED);

    @Test
    void completedMemberCanReadSummaryAfterLastPlanDate() {
        LocalDate date = LocalDate.of(2026, 9, 19);
        Study study = Study.builder().studyTitle("완료한 스터디")
                .startDate(date.minusDays(2)).endDate(date.minusDays(1)).build();
        StudyMember member = StudyMember.builder().studyMemberId(3L)
                .study(study).status(StudyMemberStatus.COMPLETED).build();
        when(studyRepository.findById(1L)).thenReturn(Optional.of(study));
        when(studyMemberRepository.findByStudy_StudyIdAndUser_UserIdAndStatusIn(1L, 2L, READ_STATUSES))
                .thenReturn(Optional.of(member));
        when(studyMemberRepository.findStudyMemberIdByStudyIdAndUserIdAndStatusIn(1L, 2L, CURRENT_STATUSES))
                .thenReturn(Optional.of(3L));

        var plan = dailyService.getStudyPlanInfoByDate(1L, date, 2L);
        assertThat(plan.memberStatus()).isEqualTo(StudyMemberStatus.COMPLETED);
        assertThat(plan.studyDailyPlanId()).isNull();
        assertThat(plan.studyTitle()).isEqualTo("완료한 스터디");
        assertThat(contentService.getUserContents(1L, 2L, date)).isNotNull();
        assertThat(queryService.getStudyMemberTotalIndicatorByUserId(1L, 2L)).isNotNull();
    }

    @Test
    void completedMemberCanReadRecentWeek() {
        Study study = Study.builder().studyTitle("완료한 스터디").build();
        when(studyMemberRepository.existsByStudy_StudyIdAndUser_UserIdAndStatusIn(1L, 2L, CURRENT_STATUSES))
                .thenReturn(true);
        when(studyRepository.findByIdWithStudyMembersAndUser(1L)).thenReturn(Optional.of(study));
        User user = mock(User.class);
        when(user.getName()).thenReturn("수료한 사용자");
        study.getStudyMembers().add(StudyMember.builder().studyMemberId(3L)
                .user(user).status(StudyMemberStatus.COMPLETED).build());
        var result = queryService.getRecentWeekStudyInfos(1L, 2L);
        assertThat(result).hasSize(1);
        assertThat(result.getFirst().studyMemberId()).isEqualTo(3L);
    }

    @Test
    void nonMemberStillCannotReadDailyPlan() {
        when(studyRepository.findById(1L)).thenReturn(Optional.of(Study.builder().build()));
        assertThatThrownBy(() -> dailyService.getStudyPlanInfoByDate(1L, LocalDate.now(), 2L))
                .isInstanceOf(StudyException.class);
    }

    @Test
    void startupFinalizesMembersAfterFailingMissedPlans() {
        StudyDailyService service = mock(StudyDailyService.class);
        new StudyDailyPlanScheduler(service).runOnStartup();
        var order = inOrder(service);
        order.verify(service).markIncompletePlansAsFailure();
        order.verify(service).finalizeExpiredStudyMembers();
    }
}