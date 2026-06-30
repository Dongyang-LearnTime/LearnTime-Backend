package learntime.backend.domain.study_plan.service.core;

import learntime.backend.domain.point.dto.PointEventDTO;
import learntime.backend.domain.point.enums.PointPolicy;
import learntime.backend.domain.point.enums.PointType;
import learntime.backend.domain.study_plan.converter.StudyDailyPlanConverter;
import learntime.backend.domain.study_plan.dto.response.StudyPlanResponseDTO;
import learntime.backend.domain.study.error.code.StudyErrorCode;
import learntime.backend.domain.study.error.exception.StudyException;
import learntime.backend.domain.study.model.Study;
import learntime.backend.domain.study.model.StudyDailyPlan;
import learntime.backend.domain.study.repository.StudyDailyPlanRepository;
import learntime.backend.domain.study.repository.StudyRepository;
import learntime.backend.domain.study_member.enums.StudyMemberRole;
import learntime.backend.domain.study_member.enums.StudyMemberStatus;
import learntime.backend.domain.study_member.enums.StudyPlanStatus;
import learntime.backend.domain.study_member.model.StudyMember;
import learntime.backend.domain.study_member.repository.StudyMemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class StudyPlanStoreService {

    private final StudyRepository studyRepository;
    private final StudyDailyPlanRepository studyDailyPlanRepository;
    private final StudyMemberRepository studyMemberRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public void saveGeneratedPlanAndEvents(Long studyId, StudyPlanResponseDTO geminiResult, List<LocalDate> planDates) {
        Study study = studyRepository.findById(studyId)
                .orElseThrow(() -> new StudyException(StudyErrorCode.STUDY_NOT_FOUND));

        List<StudyDailyPlan> dailyPlans = new ArrayList<>();
        for (int i = 0; i < geminiResult.dailyPlans().size(); i++) {
            var planDto = geminiResult.dailyPlans().get(i);
            dailyPlans.add(StudyDailyPlanConverter.toStudyDailyPlanEntity(study, planDto, planDates.get(i)));
        }

        long startTime = System.currentTimeMillis();
        studyDailyPlanRepository.saveAll(dailyPlans);
        long endTime = System.currentTimeMillis();
        log.info("[StudyPlan Save] {}일 분량의 계획(복습 포함) 저장 완료. 스터디ID: {}, 소요 시간: {}ms",
                dailyPlans.size(), study.getStudyId(), (endTime - startTime));

        study.updateStatus(StudyPlanStatus.READY);
        studyRepository.save(study);

        List<StudyMember> members = studyMemberRepository.findAllByStudy_StudyIdAndStatus(
                study.getStudyId(),
                StudyMemberStatus.ACTIVE
        );
        for (StudyMember member : members) {
            PointPolicy policy = member.getStudyMemberRole() == StudyMemberRole.OWNER
                    ? PointPolicy.STUDY_PLAN_CREATED
                    : PointPolicy.STUDY_PLAN_JOINED;

            eventPublisher.publishEvent(new PointEventDTO(
                    member.getUser().getUserId(),
                    policy.getAmount(),
                    PointType.EARN,
                    policy.getDescription()
            ));
        }
    }

    @Transactional
    public void updateStudyStatusFailed(Long studyId) {
        studyRepository.findById(studyId).ifPresent(s -> {
            s.updateStatus(StudyPlanStatus.FAILED);
            studyRepository.save(s);
        });
    }
}
