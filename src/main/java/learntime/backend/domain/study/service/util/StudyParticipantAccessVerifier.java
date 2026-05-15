package learntime.backend.domain.study.service.util;

import learntime.backend.domain.study.error.code.StudyErrorCode;
import learntime.backend.domain.study.error.exception.StudyException;
import learntime.backend.domain.study.model.StudyDailyPlan;
import learntime.backend.domain.study.model.StudyParticipant;
import learntime.backend.global.error.code.ErrorCode;
import learntime.backend.global.error.exception.BusinessException;

public final class StudyParticipantAccessVerifier {

    private StudyParticipantAccessVerifier() {
        throw new BusinessException(ErrorCode.UTILITY_CLASS_INSTANTIATION);
    }

    public static void verifyDailyPlanParticipant(StudyDailyPlan studyDailyPlan, Long userId) {
        StudyParticipant participant = studyDailyPlan.getStudyParticipant();

        // 신규 공유 구조는 StudyParticipant 기준으로 권한을 판단합니다.
        if (participant != null) {
            if (participant.getLeftAt() == null && participant.getUser().getUserId().equals(userId)) {
                return;
            }
            throw new StudyException(StudyErrorCode.STUDY_UNAUTHORIZED_ACCESS);
        }

        // 기존 데이터는 Study 소유자 기준으로 호환 처리합니다.
        if (!studyDailyPlan.getStudy().getUser().getUserId().equals(userId)) {
            throw new StudyException(StudyErrorCode.STUDY_UNAUTHORIZED_ACCESS);
        }
    }
}
