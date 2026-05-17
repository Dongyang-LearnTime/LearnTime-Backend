package learntime.backend.domain.study.service.core;

import learntime.backend.domain.study.dto.request.StudyUserContentRequestDTO;
import learntime.backend.domain.study.error.code.StudyErrorCode;
import learntime.backend.domain.study.error.exception.StudyException;
import learntime.backend.domain.study.model.StudyDailyPlan;
import learntime.backend.domain.study.model.StudyMember;
import learntime.backend.domain.study.model.StudyMemberContent;
import learntime.backend.domain.study.model.StudyStatus;
import learntime.backend.domain.study.repository.StudyDailyPlanRepository;
import learntime.backend.domain.study.repository.StudyMemberRepository;
import learntime.backend.domain.study.repository.StudyStatusRepository;
import learntime.backend.domain.study.repository.StudyUserContentRepository;
import lombok.RequiredArgsConstructor;
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

    /** 사용자의 오늘 공부 내용을 저장하거나 수정합니다. */
    @Transactional
    public Long upsertUserContent(StudyUserContentRequestDTO request, Long userId) {
        StudyDailyPlan dailyPlan = studyDailyPlanRepository.findById(request.studyDailyPlanId())
                .orElseThrow(() -> new StudyException(StudyErrorCode.STUDY_DAILY_NOT_FOUND));

        StudyMember member = studyMemberRepository.findByStudy_StudyIdAndUser_UserId(
                        dailyPlan.getStudy().getStudyId(), userId)
                .orElseThrow(() -> new StudyException(StudyErrorCode.STUDY_MEMBER_NOT_FOUND));

        StudyMemberContent content = studyUserContentRepository.findByStudyMemberAndStudyDailyPlan(member, dailyPlan)
                .orElseGet(() -> StudyMemberContent.builder()
                        .studyMember(member)
                        .studyDailyPlan(dailyPlan)
                        .build());

        content.updateContent(request.userContent());
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
        
        return content.getStudyMemberContentId();
    }
}
