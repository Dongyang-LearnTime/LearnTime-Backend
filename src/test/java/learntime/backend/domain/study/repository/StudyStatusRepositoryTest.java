package learntime.backend.domain.study.repository;

import learntime.backend.domain.study.enums.CompletionStatus;
import learntime.backend.domain.study.enums.ProgressStatus;
import learntime.backend.domain.study.model.Study;
import learntime.backend.domain.study.model.StudyDailyPlan;
import learntime.backend.domain.study.model.StudyStatus;
import learntime.backend.domain.study_member.enums.StudyMemberRole;
import learntime.backend.domain.study_member.enums.StudyMemberStatus;
import learntime.backend.domain.study_member.model.StudyMember;
import learntime.backend.domain.study_member.repository.StudyMemberRepository;
import learntime.backend.domain.user.model.User;
import learntime.backend.domain.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
public class StudyStatusRepositoryTest {

    @Autowired
    private StudyStatusRepository studyStatusRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StudyRepository studyRepository;

    @Autowired
    private StudyMemberRepository studyMemberRepository;

    @Autowired
    private StudyDailyPlanRepository studyDailyPlanRepository;

    @Test
    @DisplayName("insertMissingStatusesAsFailure - active member and past plan with no existing status should insert failure")
    void testInsertMissingStatusesAsFailure_ActiveMemberPastPlan() {
        // given
        String suffix1 = java.util.UUID.randomUUID().toString().substring(0, 8);
        User user = User.builder()
                .email("test_" + suffix1 + "@test.com")
                .name("user_" + suffix1)
                .build();
        userRepository.saveAndFlush(user);

        Study study = Study.builder()
                .studyTitle("Test Study")
                .bookTitle("Test Book")
                .startDate(LocalDate.now().minusDays(5))
                .endDate(LocalDate.now().plusDays(5))
                .build();
        studyRepository.saveAndFlush(study);

        StudyMember member = StudyMember.builder()
                .user(user)
                .study(study)
                .studyMemberRole(StudyMemberRole.MEMBER)
                .status(StudyMemberStatus.ACTIVE)
                .build();
        studyMemberRepository.saveAndFlush(member);

        StudyDailyPlan plan = StudyDailyPlan.builder()
                .dayNumber(1)
                .planDate(LocalDate.now().minusDays(2))
                .planContent("Day 1 Content")
                .study(study)
                .build();
        studyDailyPlanRepository.saveAndFlush(plan);

        // when
        int inserted = studyStatusRepository.insertMissingStatusesAsFailure(LocalDate.now(), StudyMemberStatus.ACTIVE.name());

        // then
        assertThat(inserted).isEqualTo(1);

        Optional<StudyStatus> statusOpt = studyStatusRepository.findByStudyMember_StudyMemberIdAndStudyDailyPlan_StudyDailyPlanId(
                member.getStudyMemberId(), plan.getStudyDailyPlanId()
        );
        assertThat(statusOpt).isPresent();
        assertThat(statusOpt.get().getProgressStatus()).isEqualTo(ProgressStatus.COMPLETED);
        assertThat(statusOpt.get().getCompletionStatus()).isEqualTo(CompletionStatus.FAILURE);
    }

    @Test
    @DisplayName("insertMissingStatusesAsFailure - withdrawn member should not get new failure status, and should not cause duplicate constraint violation when re-run")
    void testInsertMissingStatusesAsFailure_WithdrawnMember_NoDuplicateViolation() {
        // given
        String suffix2 = java.util.UUID.randomUUID().toString().substring(0, 8);
        User user = User.builder()
                .email("test_" + suffix2 + "@test.com")
                .name("user_" + suffix2)
                .build();
        userRepository.saveAndFlush(user);

        Study study = Study.builder()
                .studyTitle("Test Study 2")
                .bookTitle("Test Book 2")
                .startDate(LocalDate.now().minusDays(5))
                .endDate(LocalDate.now().plusDays(5))
                .build();
        studyRepository.saveAndFlush(study);

        StudyMember member = StudyMember.builder()
                .user(user)
                .study(study)
                .studyMemberRole(StudyMemberRole.MEMBER)
                .status(StudyMemberStatus.WITHDRAWN) // withdrawn member
                .build();
        studyMemberRepository.saveAndFlush(member);

        StudyDailyPlan plan = StudyDailyPlan.builder()
                .dayNumber(1)
                .planDate(LocalDate.now().minusDays(2))
                .planContent("Day 1 Content")
                .study(study)
                .build();
        studyDailyPlanRepository.saveAndFlush(plan);

        // First run (withdrawn member)
        int inserted1 = studyStatusRepository.insertMissingStatusesAsFailure(LocalDate.now(), StudyMemberStatus.ACTIVE.name());
        assertThat(inserted1).isEqualTo(0); // Should not insert for WITHDRAWN member

        // If the withdrawn member already has a study status, let's make sure it doesn't try to insert another one and violate constraint
        StudyStatus existingStatus = StudyStatus.builder()
                .studyMember(member)
                .studyDailyPlan(plan)
                .progressStatus(ProgressStatus.COMPLETED)
                .completionStatus(CompletionStatus.FAILURE)
                .build();
        studyStatusRepository.saveAndFlush(existingStatus);

        // Second run - should run fine without throwing Duplicate Entry exception
        int inserted2 = studyStatusRepository.insertMissingStatusesAsFailure(LocalDate.now(), StudyMemberStatus.ACTIVE.name());
        assertThat(inserted2).isEqualTo(0);
    }
}
