package learntime.backend.global.utils;

import learntime.backend.domain.study_member.enums.StudyMemberRole;
import learntime.backend.domain.study.error.code.StudyErrorCode;
import learntime.backend.domain.study.error.exception.StudyException;
import learntime.backend.domain.study.model.Study;
import learntime.backend.domain.study_member.model.StudyMember;
import learntime.backend.global.error.code.ErrorCode;
import learntime.backend.global.error.exception.BusinessException;

public class StudyAuthUtil {

    private StudyAuthUtil() {
        throw new BusinessException(ErrorCode.UTILITY_CLASS_INSTANTIATION);
    }

    // 현재 사용자가 해당 스터디의 멤버인지 검증함 (조회 권한 등)
    public static void verifyStudyMember(Study study, Long userId) {
        boolean isMember = study.getStudyMembers().stream()
                .anyMatch(m -> m.isActive() && m.getUser().getUserId().equals(userId));
        if (!isMember) {
            throw new StudyException(StudyErrorCode.STUDY_UNAUTHORIZED_ACCESS);
        }
    }

    // 현재 사용자가 해당 스터디 멤버 본인인지 검증함 (수정/삭제 권한 등)
    public static void verifyOwnership(StudyMember studyMember, Long userId) {
        if (!studyMember.getUser().getUserId().equals(userId)) {
            throw new StudyException(StudyErrorCode.STUDY_UNAUTHORIZED_ACCESS);
        }
    }

    // 현재 사용자가 스터디 맴버이고, 오너인지 확인
    public static void checkOwnerRole(StudyMember studyMember) {
        if (!studyMember.isActive() || studyMember.getStudyMemberRole() != StudyMemberRole.OWNER) {
            throw new StudyException(StudyErrorCode.STUDY_UNAUTHORIZED_ACCESS);
        }
    }


}
