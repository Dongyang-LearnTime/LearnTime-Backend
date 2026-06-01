package learntime.backend.domain.study.service.core;

import learntime.backend.domain.study.dto.request.UpdateStudyRestScheduleRequestDTO;
import learntime.backend.domain.study.error.code.StudyErrorCode;
import learntime.backend.domain.study.error.exception.StudyException;
import learntime.backend.domain.study.model.Study;
import learntime.backend.domain.study.model.StudyDailyPlan;
import learntime.backend.domain.study.model.StudyRestDate;
import learntime.backend.domain.study.model.StudyRestDay;
import learntime.backend.domain.study.repository.StudyDailyPlanRepository;
import learntime.backend.domain.study.repository.StudyRepository;
import learntime.backend.domain.study.repository.StudyRestDateRepository;
import learntime.backend.domain.study.repository.StudyRestDayRepository;
import learntime.backend.domain.study.service.util.StudyDateCalculator;
import learntime.backend.domain.study_member.enums.StudyPlanStatus;
import learntime.backend.domain.study_member.enums.StudyMemberStatus;
import learntime.backend.domain.study_member.model.StudyMember;
import learntime.backend.domain.study_member.repository.StudyMemberRepository;
import learntime.backend.global.utils.StudyAuthUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class StudyRestService {

    private final StudyRepository studyRepository;
    private final StudyDailyPlanRepository studyDailyPlanRepository;
    private final StudyMemberRepository studyMemberRepository;
    private final StudyRestDateRepository studyRestDateRepository;
    private final StudyRestDayRepository studyRestDayRepository;
    private final StudyRestManager studyRestManager;
    private final StudyDateCalculator studyDateCalculator;

    @Transactional
    public void updateRestSchedule(Long studyId, UpdateStudyRestScheduleRequestDTO request, Long userId) {
        Study study = studyRepository.findById(studyId)
                .orElseThrow(() -> new StudyException(StudyErrorCode.STUDY_NOT_FOUND));

        StudyMember studyMember = studyMemberRepository.findByStudy_StudyIdAndUser_UserIdAndStatus(
                        studyId,
                        userId,
                        StudyMemberStatus.ACTIVE
                )
                .orElseThrow(() -> new StudyException(StudyErrorCode.STUDY_MEMBER_NOT_FOUND));

        StudyAuthUtil.checkOwnerRole(studyMember);

        if (study.getStatus() != StudyPlanStatus.READY) {
            throw new StudyException(StudyErrorCode.STUDY_REST_UPDATE_NOT_READY);
        }

        LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));
        Set<DayOfWeek> currentRestDays = studyRestDayRepository.findAllByStudy_StudyId(studyId)
                .stream()
                .map(StudyRestDay::getDayOfWeek)
                .collect(Collectors.toSet());
        Set<LocalDate> currentRestDates = studyRestDateRepository.findAllByStudy_StudyId(studyId)
                .stream()
                .map(StudyRestDate::getRestDate)
                .collect(Collectors.toSet());

        List<DayOfWeek> newRestDays = normalizeRestDays(request.restDays());
        List<LocalDate> newRestDates = normalizeRestDates(request.restDates());
        Set<DayOfWeek> newRestDaySet = Set.copyOf(newRestDays);
        Set<LocalDate> newRestDateSet = Set.copyOf(newRestDates);

        boolean wasTodayRest = isRestDate(today, currentRestDays, currentRestDates);
        boolean willTodayRest = isRestDate(today, newRestDaySet, newRestDateSet);
        if (!wasTodayRest && willTodayRest) {
            throw new StudyException(StudyErrorCode.TODAY_REST_CHANGE_NOT_ALLOWED);
        }

        List<StudyDailyPlan> plans = studyDailyPlanRepository.findAllByStudy_StudyIdOrderByDayNumberAsc(studyId);
        List<StudyDailyPlan> futurePlans = plans.stream()
                .filter(plan -> plan.getPlanDate().isAfter(today))
                .toList();

        List<LocalDate> recalculatedDates = studyDateCalculator.buildPlanDates(
                today.plusDays(1),
                futurePlans.size(),
                newRestDaySet,
                newRestDateSet
        );

        for (int i = 0; i < futurePlans.size(); i++) {
            futurePlans.get(i).setPlanDate(recalculatedDates.get(i));
        }

        studyRestDayRepository.deleteAllByStudy_StudyId(studyId);
        studyRestDateRepository.deleteAllByStudy_StudyId(studyId);
        studyRestManager.saveRestDays(study, newRestDays);
        studyRestManager.saveRestDates(study, newRestDates);

        if (!plans.isEmpty()) {
            LocalDate lastPlanDate = plans.stream()
                    .map(StudyDailyPlan::getPlanDate)
                    .max(LocalDate::compareTo)
                    .orElse(study.getEndDate());
            study.updateStudyDates(study.getStartDate(), lastPlanDate);
        }

        log.info("[Study Rest Update] 휴무 일정 재조정 완료 - 스터디 ID: {}, 요청자 ID: {}", studyId, userId);
    }

    private List<DayOfWeek> normalizeRestDays(List<DayOfWeek> restDays) {
        if (restDays == null || restDays.isEmpty()) {
            return List.of();
        }

        return new ArrayList<>(new LinkedHashSet<>(restDays));
    }

    private List<LocalDate> normalizeRestDates(List<LocalDate> restDates) {
        if (restDates == null || restDates.isEmpty()) {
            return List.of();
        }

        return new ArrayList<>(new LinkedHashSet<>(restDates));
    }

    private boolean isRestDate(LocalDate date, Set<DayOfWeek> restDays, Set<LocalDate> restDates) {
        return restDays.contains(date.getDayOfWeek()) || restDates.contains(date);
    }
}
