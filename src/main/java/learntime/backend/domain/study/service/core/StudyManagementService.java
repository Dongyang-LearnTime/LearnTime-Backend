package learntime.backend.domain.study.service.core;

import learntime.backend.domain.point.dto.PointEventDTO;
import learntime.backend.domain.point.enums.PointPolicy;
import learntime.backend.domain.point.enums.PointType;
import learntime.backend.domain.study.converter.StudyConverter;
import learntime.backend.domain.study.dto.request.GeminiStudyRequestDTO;
import learntime.backend.domain.study.dto.response.StudyPlanResponseDTO;
import learntime.backend.domain.studymember.enums.StudyPlanStatus;
import learntime.backend.domain.studymember.enums.StudyMemberRole;
import learntime.backend.domain.study.error.code.StudyErrorCode;
import learntime.backend.domain.study.error.exception.StudyException;
import learntime.backend.domain.study.model.Study;
import learntime.backend.domain.study.model.StudyDailyPlan;
import learntime.backend.domain.studymember.model.StudyMember;
import learntime.backend.domain.study.repository.StudyDailyPlanRepository;
import learntime.backend.domain.studymember.repository.StudyMemberRepository;
import learntime.backend.domain.study.repository.StudyRepository;
import learntime.backend.domain.study.service.ai.GeminiStudyService;
import learntime.backend.domain.study.service.util.StudyDateCalculator;
import learntime.backend.domain.user.model.User;
import learntime.backend.domain.user.repository.UserRepository;
import learntime.backend.global.error.code.AuthErrorCode;
import learntime.backend.global.error.code.ErrorCode;
import learntime.backend.global.error.exception.AuthException;
import learntime.backend.global.error.exception.BusinessException;
import learntime.backend.global.utils.PromptQuotaUtil;
import learntime.backend.global.utils.StudyAuthUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

// 스터디 생성, 재생성, 초기화 등 상태를 변경하는 로직 담당 서비스
@Slf4j
@Service
@RequiredArgsConstructor
public class StudyManagementService {

    private final StudyRepository studyRepository;
    private final StudyDailyPlanRepository studyDailyPlanRepository;
    private final StudyMemberRepository studyMemberRepository;
    private final UserRepository userRepository;
    private final StudyRestManager studyRestManager;
    private final PromptQuotaUtil promptQuotaUtil;
    private final StudyDateCalculator studyDateCalculator;
    private final ApplicationEventPublisher eventPublisher;
    private final GeminiStudyService geminiStudyService;

    /**
     * 초기 상태(PLANNING)로 스터디 정보를 먼저 저장함 (비동기 처리 준비)
     */
    @Transactional
    public Long initializeStudy(GeminiStudyRequestDTO request, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthException(AuthErrorCode.USER_NOT_FOUND));

        try {
            Study study = StudyConverter.toStudyEntity(request, user);
            studyRepository.save(study);

            // 스터디 생성자 (Owner) 저장
            StudyMember owner = StudyMember.builder()
                    .user(user)
                    .study(study)
                    .studyMemberRole(StudyMemberRole.OWNER)
                    .build();

            studyMemberRepository.save(owner);

            // 추가 멤버 저장
            if (request.studyMemberList() != null && !request.studyMemberList().isEmpty()) {
                List<User> additionalUsers = userRepository.findAllById(request.studyMemberList());
                List<StudyMember> studyMembers = additionalUsers.stream()
                        .filter(m -> !m.getUserId().equals(userId))
                        .map(m -> StudyMember.builder()
                                .user(m)
                                .study(study)
                                .studyMemberRole(StudyMemberRole.MEMBER)
                                .build())
                        .toList();
                studyMemberRepository.saveAll(studyMembers);
            }

            // 쉬는 날짜 정보 저장
            studyRestManager.saveRestDates(study, request.restDates());
            studyRestManager.saveRestDays(study, request.restDays());

            return study.getStudyId();

        } catch (Exception e) {
            log.error("스터디 초기화 실패", e);
            throw new StudyException(StudyErrorCode.STUDY_SAVE_FAILED);
        }
    }

    /**
     * 비동기로 AI 계획 생성 및 상세 일정 저장
     */
    @Async
    @Transactional
    public void generateAndSavePlanAsync(Long studyId, GeminiStudyRequestDTO request, Long userId) {
        Study study = null;
        try {
            study = studyRepository.findById(studyId)
                    .orElseThrow(() -> new StudyException(StudyErrorCode.STUDY_NOT_FOUND));

            // 1. AI 호출 (단순 목차 분배)
            StudyPlanResponseDTO geminiResult = geminiStudyService.generateSmartStudyPlan(request, userId);

            // 2. 실제 학습 날짜 계산 (서버 로직)
            List<LocalDate> planDates = buildPlanDatesFromRequest(
                    request.startDate(),
                    geminiResult.dailyPlans().size(),
                    request.restDays(),
                    request.restDates()
            );

            // 3. 일차별 상세 계획 생성 (대량 저장 준비)
            List<StudyDailyPlan> dailyPlans = new ArrayList<>();
            for (int i = 0; i < geminiResult.dailyPlans().size(); i++) {
                var planDto = geminiResult.dailyPlans().get(i);
                dailyPlans.add(StudyConverter.toStudyDailyPlanEntity(study, planDto, planDates.get(i)));
            }

            // 4. JPA saveAll 활용한 대량 저장 (Batch Insert 최적화)
            long startTime = System.currentTimeMillis();
            studyDailyPlanRepository.saveAll(dailyPlans);
            long endTime = System.currentTimeMillis();
            log.info("[StudyPlan Save] {}일 분량의 계획(복습 포함) 저장 완료. 스터디ID: {}, 소요 시간: {}ms", 
                    dailyPlans.size(), study.getStudyId(), (endTime - startTime));

            // 5. 상태 업데이트: PLANNING -> READY
            study.updateStatus(StudyPlanStatus.READY);
            studyRepository.save(study);

            // 6. 모든 멤버에게 포인트 지급
            List<StudyMember> members = studyMemberRepository.findAllByStudy_StudyId(study.getStudyId());
            for (StudyMember member : members) {
                PointPolicy policy = (member.getStudyMemberRole() == StudyMemberRole.OWNER)
                        ? PointPolicy.STUDY_PLAN_CREATED 
                        : PointPolicy.STUDY_PLAN_JOINED;
                
                eventPublisher.publishEvent(new PointEventDTO(
                        member.getUser().getUserId(), 
                        policy.getAmount(), 
                        PointType.EARN, 
                        policy.getDescription()
                ));
            }

        } catch (Exception e) {
            log.error("비동기 학습 계획 생성 실패", e);
            promptQuotaUtil.restorePromptQuota(userId);
            if (study != null) {
                study.updateStatus(StudyPlanStatus.FAILED);
                studyRepository.save(study);
            }
        }
    }

    // 시작일과 휴일 정보를 바탕으로 실제 학습 날짜 목록을 생성함
    private List<LocalDate> buildPlanDatesFromRequest(
            LocalDate startDate,
            int planSize,
            List<DayOfWeek> restDays,
            List<LocalDate> restDates
    ) {
        Set<DayOfWeek> restDaysSet =
                restDays == null ? Set.of() : Set.copyOf(restDays);

        Set<LocalDate> restDatesSet =
                restDates == null ? Set.of() : Set.copyOf(restDates);

        return studyDateCalculator.buildPlanDates(
                startDate,
                planSize,
                restDaysSet,
                restDatesSet
        );
    }

    /**
     * 스터디와 관련된 모든 데이터를 벌크 삭제함.
     * (단, StudyNotes는 SET NULL 제약에 의해 데이터가 유지됨.)
     */
    @Transactional
    public void deleteStudyBulk(Long studyId, Long userId) {
        boolean existsStudy = studyRepository.existsById(studyId);
        if (!existsStudy) {
            throw new StudyException(StudyErrorCode.STUDY_NOT_FOUND);
        }

        StudyMember studyMember = studyMemberRepository.findByStudy_StudyIdAndUser_UserId(studyId, userId)
                .orElseThrow(() -> new StudyException(StudyErrorCode.STUDY_MEMBER_NOT_FOUND));

        // 방장 권한 검증
        StudyAuthUtil.checkOwnerRole(studyMember);

        // 1. 가장 하위 계층(1계층) 벌크 삭제
        studyRepository.deleteStudyMemberContentsByStudyId(studyId);
        studyRepository.deleteStudyStatusesByStudyId(studyId);
        studyRepository.deleteStudyFeedbacksByStudyId(studyId);
        studyRepository.deleteQuizHistoriesByStudyId(studyId);
        studyRepository.deleteQuizQuestionsByStudyId(studyId);

        // 2. 2계층 벌크 삭제 (StudyQuiz) - StudyNotes는 보존(Set Null)
        studyRepository.deleteStudyQuizzesByStudyId(studyId);

        // 3. 3계층(Study와 직접 연관된 하위) 벌크 삭제
        studyRepository.deleteStudyDailyPlansByStudyId(studyId);
        studyRepository.deleteStudyRestDatesByStudyId(studyId);
        studyRepository.deleteStudyRestDaysByStudyId(studyId);
        studyRepository.deleteStudyInvitationsByStudyId(studyId);

        // 4. StudyMember 및 Study 본체 삭제
        studyRepository.deleteStudyMembersByStudyId(studyId);
        studyRepository.deleteStudyById(studyId);

        log.info("[Study Delete] 스터디 벌크 삭제 완료 - 스터디 ID: {}, 요청자 ID: {}", studyId, userId);
    }
}
