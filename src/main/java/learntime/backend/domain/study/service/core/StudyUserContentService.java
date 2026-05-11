package learntime.backend.domain.study.service.core;

import learntime.backend.domain.study.dto.request.StudyUserContentRequestDTO;
import learntime.backend.domain.study.enums.ProgressStatus;
import learntime.backend.domain.study.error.code.StudyErrorCode;
import learntime.backend.domain.study.error.exception.StudyException;
import learntime.backend.domain.study.model.StudyDailyPlan;
import learntime.backend.domain.study.model.StudyUserContent;
import learntime.backend.domain.study.repository.StudyDailyPlanRepository;
import learntime.backend.domain.study.repository.StudyUserContentRepository;
import learntime.backend.global.utils.AuthorizationUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StudyUserContentService {

    private final StudyUserContentRepository studyUserContentRepository;
    private final StudyDailyPlanRepository studyDailyPlanRepository;

    // 일일 학습 계획에 사용자가 작성한 학습 내용을 추가합니다.
    @Transactional
    public Long createUserContent(StudyUserContentRequestDTO request, Long userId) {
        StudyDailyPlan dailyPlan = studyDailyPlanRepository.findById(request.studyDailyPlanId())
                .orElseThrow(() -> new StudyException(StudyErrorCode.STUDY_DAILY_NOT_FOUND));

        // 이미 완료된 공부 일정이라면 내용 추가 금지
        if (dailyPlan.getProgressStatus().equals(ProgressStatus.COMPLETED)) {
            throw new StudyException(StudyErrorCode.STUDY_DAILY_ALREADY_COMPLETED);
        }

        AuthorizationUtil.verifyOwnership(userId, dailyPlan.getStudy().getUser().getUserId());

        StudyUserContent userContent = StudyUserContent.builder()
                .studyDailyPlan(dailyPlan)
                .userContent(request.userContent())
                .build();

        return studyUserContentRepository.save(userContent).getStudyUserContentId();
    }
}
