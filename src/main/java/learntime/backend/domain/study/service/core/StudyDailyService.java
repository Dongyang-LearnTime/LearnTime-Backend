package learntime.backend.domain.study.service.core;

import learntime.backend.domain.point.dto.PointEventDTO;
import learntime.backend.domain.point.enums.PointPolicy;
import learntime.backend.domain.point.enums.PointType;
import learntime.backend.domain.study.converter.StudyDailyPlanConverter;
import learntime.backend.domain.study.dto.request.PlanCompleteRequestDTO;
import learntime.backend.domain.study.dto.response.StudyDailyPlanInfoResponseDTO;
import learntime.backend.domain.study.dto.response.StudyDailyPlanResponseDTO;
import learntime.backend.domain.study.enums.CompletionStatus;
import learntime.backend.domain.study.enums.ProgressStatus;
import learntime.backend.domain.study.error.code.StudyErrorCode;
import learntime.backend.domain.study.error.exception.StudyException;
import learntime.backend.domain.study.model.Study;
import learntime.backend.domain.study.model.StudyDailyPlan;
import learntime.backend.domain.study.repository.StudyDailyPlanRepository;
import learntime.backend.domain.study.repository.StudyRepository;
import learntime.backend.domain.study.repository.StudyRestDateRepository;
import learntime.backend.domain.study.repository.StudyRestDayRepository;
import learntime.backend.global.utils.StudyAuthUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import learntime.backend.domain.study.model.StudyRestDay;
import learntime.backend.domain.study.model.StudyRestDate;

import learntime.backend.domain.study.model.StudyStatus;
import learntime.backend.domain.study_member.model.StudyMember;
import learntime.backend.domain.study.repository.StudyStatusRepository;
import learntime.backend.domain.study_member.repository.StudyMemberRepository;
import learntime.backend.domain.study.dto.response.TodayStudyPlanResponseDTO;
import learntime.backend.domain.user.repository.UserRepository;
import learntime.backend.domain.user.model.User;
import learntime.backend.domain.user.converter.UserConverter;
import learntime.backend.global.error.code.AuthErrorCode;
import learntime.backend.global.error.exception.AuthException;

import java.util.ArrayList;
import java.util.Map;
import java.util.stream.Collectors;

// 일일 진도 및 포인트 지급 관련 비즈니스 로직 담당 서비스
@Slf4j
@Service
@RequiredArgsConstructor
public class StudyDailyService {

    private final StudyDailyPlanRepository studyDailyPlanRepository;
    private final StudyRestDateRepository studyRestDateRepository;
    private final StudyRestDayRepository studyRestDayRepository;
    private final StudyStatusRepository studyStatusRepository;
    private final StudyRepository studyRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final UserRepository userRepository;
    private final StudyMemberRepository studyMemberRepository;

    private static final int UNDERSTANDING_SCORE_WEIGHT = 2; // 이해도에 따른 가중치 (이해도 2면 10*2)

    // 특정 날짜의 학습 계획 정보를 조회합니다.
    @Transactional(readOnly = true)
    public StudyDailyPlanInfoResponseDTO getStudyPlanInfoByDate(Long studyId, LocalDate planDate, Long userId) {
        Study study = studyRepository.findById(studyId)
                .orElseThrow(() -> new StudyException(StudyErrorCode.STUDY_DAILY_NOT_FOUND));

        List<DayOfWeek> restDays = studyRestDayRepository.findAllByStudy_StudyId(studyId)
                .stream().map(StudyRestDay::getDayOfWeek).toList();

        List<LocalDate> restDates = studyRestDateRepository.findAllByStudy_StudyId(studyId)
                .stream().map(StudyRestDate::getRestDate).toList();

        StudyDailyPlan studyDailyPlan = studyDailyPlanRepository.findByStudyIdAndPlanDate(studyId, planDate)
                .orElse(null);

        Long studyMemberId = study.getStudyMembers().stream()
                .filter(m -> m.getUser().getUserId().equals(userId))
                .filter(StudyMember::isActive)
                .map(StudyMember::getStudyMemberId)
                .findFirst()
                .orElse(null);

        List<Long> allStudyMemberIds = study.getStudyMembers().stream()
                .filter(StudyMember::isActive)
                .map(StudyMember::getStudyMemberId)
                .toList();

        StudyStatus studyStatus = null;
        if (studyDailyPlan != null && studyMemberId != null) {
            studyStatus = studyStatusRepository.
                    findByStudyMember_StudyMemberIdAndStudyDailyPlan_StudyDailyPlanId(studyMemberId, studyDailyPlan.getStudyDailyPlanId())
                    .orElse(null);
        }

        return StudyDailyPlanConverter.toStudyDailyPlanInfoResponseDTO
                (planDate, study, restDays, restDates, studyDailyPlan, studyStatus, studyMemberId, allStudyMemberIds);
    }

    // study id를 기준으로 모든 StudyDailyPlan의 정보를 가져옵니다.
    @Cacheable(value = "studyDailyPlans", key = "#studyId + '_' + #userId")
    public List<StudyDailyPlanResponseDTO> findAllByStudyId(Long studyId, Long userId) {
        Study study = studyRepository.findById(studyId)
                .orElseThrow(() -> new StudyException(StudyErrorCode.STUDY_DAILY_NOT_FOUND));

        StudyAuthUtil.verifyStudyMember(study, userId);

        List<StudyDailyPlan> studyDailyPlanList =
                studyDailyPlanRepository.findAllByStudy(study);

        return studyDailyPlanList.stream()
                .map(StudyDailyPlanConverter::toStudyDailyPlanResponseDTO)
                .toList();
    }


    // 일일 학습 계획을 시작(진행 중) 상태로 변경합니다.
    @Transactional
    public void startStudyDailyPlan(Long studyDailyPlanId, Long userId) {
        StudyDailyPlan studyDailyPlan = validateAndGetDailyPlan(studyDailyPlanId);
        StudyMember studyMember = validateAndGetStudyMember(studyDailyPlan.getStudy(), userId);

        StudyStatus studyStatus = studyStatusRepository.findByStudyMember_StudyMemberIdAndStudyDailyPlan_StudyDailyPlanId(studyMember.getStudyMemberId(), studyDailyPlan.getStudyDailyPlanId())
                .orElseGet(() -> StudyStatus.builder()
                        .studyMember(studyMember)
                        .studyDailyPlan(studyDailyPlan)
                        .build());

        if (studyStatus.getProgressStatus() != ProgressStatus.NOT_STARTED) {
            throw new StudyException(StudyErrorCode.STUDY_DAILY_ALREADY_STARTED);
        }

        studyStatus.startPlan();
        studyStatusRepository.save(studyStatus);
    }

    // 일일 학습 계획을 완료 처리하고 포인트를 지급합니다.
    @Transactional
    public int completeStudyDailyPlan(PlanCompleteRequestDTO request, Long userId) {
        StudyDailyPlan studyDailyPlan = validateAndGetDailyPlan(request.studyDailyPlanId());
        StudyMember studyMember = validateAndGetStudyMember(studyDailyPlan.getStudy(), userId);

        StudyStatus studyStatus = studyStatusRepository.findByStudyMember_StudyMemberIdAndStudyDailyPlan_StudyDailyPlanId(studyMember.getStudyMemberId(), studyDailyPlan.getStudyDailyPlanId())
                .orElseGet(() -> StudyStatus.builder()
                        .studyMember(studyMember)
                        .studyDailyPlan(studyDailyPlan)
                        .build());

        // 상태가 완료됨 이거나 실패, 성공 상태가 아니고, 진행 중인 것만 완료되게 수정
        if (studyStatus.getProgressStatus() == ProgressStatus.COMPLETED
                || studyStatus.getCompletionStatus() == CompletionStatus.SUCCESS
                || studyStatus.getCompletionStatus() == CompletionStatus.FAILURE) {
            throw new StudyException(StudyErrorCode.STUDY_DAILY_ALREADY_COMPLETED);
        }

        if (studyStatus.getProgressStatus() != ProgressStatus.IN_PROGRESS) {
            throw new StudyException(StudyErrorCode.STUDY_DAILY_NOT_YET_STARTED);
        }

        studyStatus.completePlan(request.completionStatus(), request.understandingScore());
        studyStatus.setCompletionDate(LocalDateTime.now());
        
        studyStatusRepository.save(studyStatus);

        int calculatedPoint = calculatePoint(request.completionStatus(), request.understandingScore());
        String description = determineDescription(request.completionStatus(), request.understandingScore());

        eventPublisher.publishEvent(new PointEventDTO(
                userId,
                calculatedPoint,
                PointType.EARN,
                description
        ));
        
        eventPublisher.publishEvent(new learntime.backend.domain.badge.event.StudyCompletedEvent(userId, LocalDateTime.now()));

        return calculatedPoint;
    }

    // 학습 완료 상태와 이해도에 따라 지급할 포인트를 계산합니다.
    private int calculatePoint(CompletionStatus status, int understandingScore) {
        if (status == CompletionStatus.SUCCESS) {
            int bonus = understandingScore * UNDERSTANDING_SCORE_WEIGHT;
            return PointPolicy.STUDY_COMPLETED_SUCCESS.getAmount() + bonus;
        }
        return PointPolicy.STUDY_COMPLETED_FAILURE.getAmount();
    }

    // 포인트 지급 내역에 표시될 설명을 결정합니다.
    private String determineDescription(CompletionStatus status, int understandingScore) {
        if (status == CompletionStatus.SUCCESS) {
            return String.format("%s (이해도: %d점)",
                    PointPolicy.STUDY_COMPLETED_SUCCESS.getDescription(), understandingScore);
        }
        return PointPolicy.STUDY_COMPLETED_FAILURE.getDescription();
    }

    // 완료되지 않은 진도를 실패 처리함
    @Transactional
    public void markIncompletePlansAsFailure() {
        LocalDate today = LocalDate.now();

        log.info("[StudyDailyPlan] 미완료 계획 실패 처리 시작 - 기준일: {}", today);
        long startTime = System.currentTimeMillis();

        // 미생성된 상태를 FAILURE로 일괄 생성함
        int insertedCount = studyStatusRepository.insertMissingStatusesAsFailure(today);
        
        // 미완료된 상태를 FAILURE로 업데이트함
        int updatedCount = studyStatusRepository.bulkFailIncompleteStatuses(today);

        long endTime = System.currentTimeMillis();
        log.info(
                "[StudyDailyPlan] 실패 처리 완료 - 신규 생성: {}건, 상태 업데이트: {}건, 소요 시간: {}ms",
                insertedCount,
                updatedCount,
                (endTime - startTime)
        );
    }

    // 특정 일일 학습 계획을 검증하고 조회합니다.
    private StudyDailyPlan validateAndGetDailyPlan(Long studyDailyPlanId) {
        StudyDailyPlan studyDailyPlan = studyDailyPlanRepository.findById(studyDailyPlanId)
                .orElseThrow(() -> new StudyException(StudyErrorCode.STUDY_DAILY_NOT_FOUND));

        LocalDate today = LocalDate.now(java.util.TimeZone.getTimeZone("Asia/Seoul").toZoneId());
        if (studyDailyPlan.getPlanDate().isAfter(today)) {
            throw new StudyException(StudyErrorCode.STUDY_DAILY_NOT_YET_STARTED);
        }
        return studyDailyPlan;
    }

    // 스터디 멤버 권한을 검증하고 스터디 멤버를 조회합니다.
    private StudyMember validateAndGetStudyMember(Study study, Long userId) {
        return study.getStudyMembers().stream()
                .filter(m -> m.getUser().getUserId().equals(userId))
                .filter(StudyMember::isActive)
                .findFirst()
                .orElseThrow(() -> new StudyException(StudyErrorCode.STUDY_MEMBER_NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public List<TodayStudyPlanResponseDTO> getTodayPlans(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AuthException(AuthErrorCode.USER_NOT_FOUND));

        List<StudyMember> activeMembers = studyMemberRepository.findAllActiveByUserIdFetchStudy(user.getUserId());
        if (activeMembers.isEmpty()) {
            return List.of();
        }

        List<Long> studyIds = activeMembers.stream()
                .map(sm -> sm.getStudy().getStudyId())
                .toList();

        List<Long> memberIds = activeMembers.stream()
                .map(StudyMember::getStudyMemberId)
                .toList();

        LocalDate today = LocalDate.now();

        List<StudyDailyPlan> dailyPlans = studyDailyPlanRepository.findAllByStudyIdInAndPlanDate(studyIds, today);
        Map<Long, StudyDailyPlan> studyIdToPlanMap = dailyPlans.stream()
                .collect(Collectors.toMap(p -> p.getStudy().getStudyId(), p -> p));

        List<StudyStatus> studyStatuses = studyStatusRepository.findByStudyMemberIdInAndPlanDate(memberIds, today);
        Map<String, StudyStatus> statusMap = studyStatuses.stream()
                .collect(Collectors.toMap(
                        s -> s.getStudyMember().getStudyMemberId() + "_" + s.getStudyDailyPlan().getStudyDailyPlanId(),
                        s -> s
                ));

        List<TodayStudyPlanResponseDTO> response = new ArrayList<>();
        for (StudyMember member : activeMembers) {
            StudyDailyPlan plan = studyIdToPlanMap.get(member.getStudy().getStudyId());
            if (plan != null) {
                StudyStatus status = statusMap.get(member.getStudyMemberId() + "_" + plan.getStudyDailyPlanId());
                response.add(UserConverter.toTodayStudyPlanResponseDTO(member, plan, status));
            }
        }
        return response;
    }

}
