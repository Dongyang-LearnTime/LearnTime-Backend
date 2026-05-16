package learntime.backend.domain.study.service.core;

import learntime.backend.domain.study.dto.request.StudyUserContentRequestDTO;
import learntime.backend.domain.study.enums.ProgressStatus;
import learntime.backend.domain.study.error.code.StudyErrorCode;
import learntime.backend.domain.study.error.exception.StudyException;
import learntime.backend.domain.study.model.StudyDailyPlan;
import learntime.backend.domain.study.model.StudyMember;
import learntime.backend.domain.study.model.StudyMemberContent;
import learntime.backend.domain.study.model.StudyStatus;
import learntime.backend.domain.study.repository.StudyDailyPlanRepository;
import learntime.backend.domain.study.repository.StudyStatusRepository;
import learntime.backend.domain.study.repository.StudyUserContentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StudyUserContentService {

    private final StudyUserContentRepository studyUserContentRepository;
    private final StudyDailyPlanRepository studyDailyPlanRepository;
    private final StudyStatusRepository studyStatusRepository;

    // 일일 학습 계획에 사용자가 작성한 학습 내용을 추가합니다.
    @Transactional
    public Long createUserContent(StudyUserContentRequestDTO request, Long userId) {
        StudyDailyPlan dailyPlan = studyDailyPlanRepository.findById(request.studyDailyPlanId())
                .orElseThrow(() -> new StudyException(StudyErrorCode.STUDY_DAILY_NOT_FOUND));

        StudyMember studyMember = dailyPlan.getStudy().getStudyMembers().stream()
                .filter(m -> m.getUser().getUserId().equals(userId))
                .findFirst()
                .orElseThrow(() -> new StudyException(StudyErrorCode.STUDY_MEMBER_NOT_FOUND));

        StudyStatus studyStatus = studyStatusRepository.findByStudyMember_StudyMemberIdAndStudyDailyPlan_StudyDailyPlanId(studyMember.getStudyMemberId(), dailyPlan.getStudyDailyPlanId())
                .orElse(null);

        // 이미 완료된 공부 일정이라면 내용 추가를 금지함
        if (studyStatus != null && studyStatus.getProgressStatus() == ProgressStatus.COMPLETED) {
            throw new StudyException(StudyErrorCode.STUDY_DAILY_ALREADY_COMPLETED);
        }

        StudyMemberContent userContent = StudyMemberContent.builder()
                .studyMember(studyMember)
                .studyDailyPlan(dailyPlan)
                .memberContent(request.userContent())
                .build();

        return studyUserContentRepository.save(userContent).getStudyMemberContentId();
    }
}
