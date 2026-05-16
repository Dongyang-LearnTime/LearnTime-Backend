package learntime.backend.domain.study.service.core;

import learntime.backend.domain.point.dto.PointEventDTO;
import learntime.backend.domain.point.enums.PointPolicy;
import learntime.backend.domain.point.enums.PointType;
import learntime.backend.domain.study.converter.StudyConverter;
import learntime.backend.domain.study.dto.request.GeminiReplanRequestDTO;
import learntime.backend.domain.study.dto.request.GeminiStudyRequestDTO;
import learntime.backend.domain.study.dto.response.StudyPlanResponseDTO;
import learntime.backend.domain.study.enums.ProgressStatus;
import learntime.backend.domain.study.enums.StudyRole;
import learntime.backend.domain.study.error.code.StudyErrorCode;
import learntime.backend.domain.study.error.exception.StudyException;
import learntime.backend.domain.study.model.Study;
import learntime.backend.domain.study.model.StudyDailyPlan;
import learntime.backend.domain.study.model.StudyMember;
import learntime.backend.domain.study.repository.StudyDailyPlanRepository;
import learntime.backend.domain.study.repository.StudyMemberRepository;
import learntime.backend.domain.study.repository.StudyRepository;
import learntime.backend.domain.study.service.util.StudyDateCalculator;
import learntime.backend.domain.user.model.User;
import learntime.backend.domain.user.repository.UserRepository;
import learntime.backend.global.error.code.AuthErrorCode;
import learntime.backend.global.error.code.ErrorCode;
import learntime.backend.global.error.exception.AuthException;
import learntime.backend.global.error.exception.BusinessException;
import learntime.backend.global.utils.AuthorizationUtil;
import learntime.backend.global.utils.PromptQuotaUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

// 스터디 생성, 재생성, 초기화 등 상태를 변경하는 로직 담당 서비스
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

    // AI가 생성한 학습 계획을 검토 후 데이터베이스에 저장합니다.
    @Transactional
    public Long saveStudyPlan(GeminiStudyRequestDTO request,
                              StudyPlanResponseDTO geminiResult,
                              Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthException(AuthErrorCode.USER_NOT_FOUND));

        try {
            Study study = StudyConverter.toStudyEntity(request, user);
            studyRepository.save(study);

            // 스터디 생성자 (Owner) 저장
            StudyMember owner = StudyMember.builder()
                    .user(user)
                    .study(study)
                    .studyRole(StudyRole.Owner)
                    .build();

            List<StudyMember> studyMembers = new ArrayList<>();
            studyMembers.add(owner);

            // 요청에 스터디 멤버가 있다면 중복을 제거하고 추가 멤버 저장함
            if (request.studyMemberList() != null && !request.studyMemberList().isEmpty()) {
                List<Long> distinctMemberIds = request.studyMemberList().stream()
                        .distinct()
                        .toList();

                List<User> additionalUsers = userRepository.findAllById(distinctMemberIds);
                for (User additionalUser : additionalUsers) {
                    if (!additionalUser.getUserId().equals(userId)) {
                        StudyMember member = StudyMember.builder()
                                .user(additionalUser)
                                .study(study)
                                .studyRole(StudyRole.Member)
                                .build();
                        studyMembers.add(member);
                    }
                }
            }
            studyMemberRepository.saveAll(studyMembers);

            // 쉬는 날짜 정보 저장
            studyRestManager.saveRestDates(study, request.restDates());
            studyRestManager.saveRestDays(study, request.restDays());

            // 쉬는 날짜를 제외한 학습 가능한 날짜 계산
            List<LocalDate> planDates = buildPlanDatesFromRequest(
                    request.startDate(),
                    geminiResult.dailyPlans().size(),
                    request.restDays(),
                    request.restDates()
            );

            List<StudyDailyPlan> dailyPlans = new ArrayList<>(geminiResult.dailyPlans().size());

            for (int i = 0; i < geminiResult.dailyPlans().size(); i++) {
                var planDto = geminiResult.dailyPlans().get(i);
                dailyPlans.add(StudyConverter.toStudyDailyPlanEntity(study, planDto, planDates.get(i)));
            }
            studyDailyPlanRepository.saveAll(dailyPlans);

            eventPublisher.publishEvent(
                    new PointEventDTO(userId,
                            PointPolicy.STUDY_PLAN_CREATED.getAmount(),
                            PointType.EARN,
                            PointPolicy.STUDY_PLAN_CREATED.getDescription()
                    )
            );

            // 참여자들에게도 포인트 지급
            if (request.studyMemberList() != null && !request.studyMemberList().isEmpty()) {
                for (Long memberId : request.studyMemberList()) {
                    if (!memberId.equals(userId)) {
                        eventPublisher.publishEvent(
                                new PointEventDTO(memberId,
                                        PointPolicy.STUDY_PLAN_JOINED.getAmount(),
                                        PointType.EARN,
                                        PointPolicy.STUDY_PLAN_JOINED.getDescription()
                                )
                        );
                    }
                }
            }

            return owner.getStudyMemberId();

        } catch (Exception e) {
            promptQuotaUtil.restorePromptQuota(userId);
            throw new StudyException(StudyErrorCode.STUDY_SAVE_FAILED);
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
}
