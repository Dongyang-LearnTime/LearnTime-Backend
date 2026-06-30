package learntime.backend.domain.study_progress.service;

import learntime.backend.domain.study.converter.StudyConverter;
import learntime.backend.domain.study_progress.dto.request.StudyUserContentRequestDTO;
import learntime.backend.domain.study_progress.dto.request.StudyUserContentUpdateRequestDTO;
import learntime.backend.domain.study_progress.dto.response.StudyMemberContentResponseDTO;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.DayOfWeek;
import java.util.List;
import java.util.Optional;
import learntime.backend.domain.badge.event.NoteUploadedEvent;

import learntime.backend.domain.study.error.code.StudyErrorCode;
import learntime.backend.domain.study.error.exception.StudyException;
import learntime.backend.domain.study.model.StudyDailyPlan;
import learntime.backend.domain.study_plan.model.StudyRestDay;
import learntime.backend.domain.study_plan.model.StudyRestDate;
import learntime.backend.domain.study_member.enums.StudyMemberStatus;
import learntime.backend.domain.study_member.model.StudyMember;
import learntime.backend.domain.study_progress.model.StudyMemberContent;
import learntime.backend.domain.study_progress.model.StudyStatus;
import learntime.backend.domain.study.repository.StudyDailyPlanRepository;
import learntime.backend.domain.study_member.repository.StudyMemberRepository;
import learntime.backend.domain.study_progress.repository.StudyStatusRepository;
import learntime.backend.domain.study_progress.repository.StudyUserContentRepository;
import learntime.backend.domain.study_plan.repository.StudyRestDayRepository;
import learntime.backend.domain.study_plan.repository.StudyRestDateRepository;
import learntime.backend.global.utils.StudyAuthUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 사용자가 입력한 공부 내용(필기 등)을 관리하는 서비스
@Service
@RequiredArgsConstructor
public class StudyUserContentService {

    private final StudyUserContentRepository studyUserContentRepository;
    private final StudyDailyPlanRepository studyDailyPlanRepository;
    private final StudyMemberRepository studyMemberRepository;
    private final StudyStatusRepository studyStatusRepository;
    private final StudyRestDayRepository studyRestDayRepository;
    private final StudyRestDateRepository studyRestDateRepository;
    private final ApplicationEventPublisher eventPublisher;

    /** 사용자의 오늘 공부 내용을 추가합니다. ACTIVE 멤버 전용. */
    @Transactional
    public Long addUserContent(StudyUserContentRequestDTO request, Long userId) {
        StudyDailyPlan dailyPlan = studyDailyPlanRepository.findById(request.studyDailyPlanId())
                .orElseThrow(() -> new StudyException(StudyErrorCode.STUDY_DAILY_NOT_FOUND));

        validateNotHoliday(dailyPlan.getStudy().getStudyId(), dailyPlan.getPlanDate());

        // ACTIVE 멤버만 공부 내용 추가 가능
        StudyMember member = studyMemberRepository.findByStudy_StudyIdAndUser_UserIdAndStatus(
                        dailyPlan.getStudy().getStudyId(),
                        userId,
                        StudyMemberStatus.ACTIVE
                )
                .orElseThrow(() -> new StudyException(StudyErrorCode.STUDY_MEMBER_NOT_FOUND));

        StudyMemberContent content = StudyMemberContent.builder()
                .studyMember(member)
                .studyDailyPlan(dailyPlan)
                .memberContent(request.userContent())
                .build();
        studyUserContentRepository.save(content);

        // 공부 내용 입력 시, 해당 계획을 진행 중으로 변경 (상태가 '시작 전'일 때만)
        StudyStatus status = studyStatusRepository.findByStudyMember_StudyMemberIdAndStudyDailyPlan_StudyDailyPlanId(
                        member.getStudyMemberId(), dailyPlan.getStudyDailyPlanId())
                .orElseGet(() -> StudyStatus.builder()
                        .studyMember(member)
                        .studyDailyPlan(dailyPlan)
                        .build());

        try {
            status.startPlan();
            studyStatusRepository.save(status);
        } catch (IllegalStateException ignored) {
            // 이미 진행 중이거나 완료된 경우 무시
        }

        eventPublisher.publishEvent(new NoteUploadedEvent(userId, LocalDateTime.now()));
        return content.getStudyMemberContentId();
    }

    /** 사용자의 일일 진도 내용을 수정합니다.
     * 탈퇴(WITHDRAWN) 멤버는 수정할 수 없습니다. */
    @Transactional
    public void updateUserContent(Long studyMemberContentId, StudyUserContentUpdateRequestDTO request, Long userId) {
        StudyMemberContent content = studyUserContentRepository.findById(studyMemberContentId)
                .orElseThrow(() -> new StudyException(StudyErrorCode.STUDY_USER_CONTENT_NOT_FOUND));

        // 작성자 본인인지 확인
        StudyAuthUtil.verifyOwnership(content.getStudyMember(), userId);

        // 탈퇴(WITHDRAWN) 멤버는 일일 공부 내용 수정 불가
        if (!content.getStudyMember().isActive()) {
            throw new StudyException(StudyErrorCode.WITHDRAWN_MEMBER_WRITE_NOT_ALLOWED);
        }

        validateNotHoliday(content.getStudyDailyPlan().getStudy().getStudyId(), content.getStudyDailyPlan().getPlanDate());

        content.updateContent(request.userContent());
    }

    private void validateNotHoliday(Long studyId, LocalDate planDate) {
        List<DayOfWeek> restDays = studyRestDayRepository.findAllByStudy_StudyId(studyId)
                .stream().map(StudyRestDay::getDayOfWeek).toList();

        List<LocalDate> restDates = studyRestDateRepository.findAllByStudy_StudyId(studyId)
                .stream().map(StudyRestDate::getRestDate).toList();

        if (restDays.contains(planDate.getDayOfWeek()) || restDates.contains(planDate)) {
            throw new StudyException(StudyErrorCode.HOLIDAY_REGISTRATION_NOT_ALLOWED);
        }
    }

    /** 사용자의 특정 일자의 공부 내용을 조회합니다.
     * 탈퇴(WITHDRAWN) 멤버도 자신의 과거 공부 내용을 조회할 수 있습니다. */
    @Transactional(readOnly = true)
    public StudyMemberContentResponseDTO getUserContents(Long studyId, Long userId, LocalDate planDate) {
        // ACTIVE + WITHDRAWN 모두 허용 — 과거 공부 내용 조회
        StudyMember member = studyMemberRepository.findByStudy_StudyIdAndUser_UserIdAndStatusIn(
                        studyId,
                        userId,
                        List.of(StudyMemberStatus.ACTIVE, StudyMemberStatus.WITHDRAWN)
                )
                .orElseThrow(() -> new StudyException(StudyErrorCode.STUDY_MEMBER_NOT_FOUND));

        List<DayOfWeek> restDays = studyRestDayRepository.findAllByStudy_StudyId(studyId)
                .stream().map(StudyRestDay::getDayOfWeek).toList();

        List<LocalDate> restDates = studyRestDateRepository.findAllByStudy_StudyId(studyId)
                .stream().map(StudyRestDate::getRestDate).toList();

        boolean isHoliday = restDays.contains(planDate.getDayOfWeek()) || restDates.contains(planDate);

        Optional<StudyDailyPlan> optionalDailyPlan =
                studyDailyPlanRepository.findByStudyIdAndPlanDate(studyId, planDate);

        // 해당 날짜의 일일 계획이 없으면 빈 DTO 반환
        if (optionalDailyPlan.isEmpty()) {
            return StudyMemberContentResponseDTO.builder()
                    .studyDailyPlanId(null)
                    .planContent(null)
                    .isHoliday(isHoliday)
                    .memberContents(List.of())
                    .build();
        }
        StudyDailyPlan dailyPlan = optionalDailyPlan.get();

        List<StudyMemberContent> contents =
                studyUserContentRepository.findAllByStudyDailyPlanAndStudyMember(
                        dailyPlan,
                        member
                );

        return StudyConverter.toStudyMemberContentResponseDTO(
                dailyPlan,
                contents,
                isHoliday
        );
    }

    /** 사용자의 일일 진도 내용을 삭제합니다. (소유자 본인만 가능) */
    @Transactional
    public void deleteUserContent(Long studyMemberContentId, Long userId) {
        StudyMemberContent content = studyUserContentRepository.findById(studyMemberContentId)
                .orElseThrow(() -> new StudyException(StudyErrorCode.STUDY_USER_CONTENT_NOT_FOUND));

        // 작성자 본인인지 확인
        StudyAuthUtil.verifyOwnership(content.getStudyMember(), userId);

        studyUserContentRepository.delete(content);
    }
}
