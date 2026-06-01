package learntime.backend.global.utils;

import learntime.backend.domain.study_member.enums.StudyMemberRole;
import learntime.backend.domain.study_member.enums.StudyMemberStatus;
import learntime.backend.domain.study.error.code.StudyErrorCode;
import learntime.backend.domain.study.error.exception.StudyException;
import learntime.backend.domain.study_member.model.StudyMember;
import learntime.backend.domain.study_member.repository.StudyMemberRepository;
import learntime.backend.global.error.code.ErrorCode;
import learntime.backend.global.error.exception.BusinessException;

import java.util.List;

public class StudyAuthUtil {

    private StudyAuthUtil() {
        throw new BusinessException(ErrorCode.UTILITY_CLASS_INSTANTIATION);
    }

    /**
     * 현재 사용자가 해당 스터디의 ACTIVE 멤버인지 검증합니다.
     * Study 엔티티 대신 studyId로 Repository를 직접 조회하므로
     * Lazy 컬렉션 또는 detached 엔티티로 인한 NPE/LazyInitializationException이 발생하지 않습니다.
     */
    public static void verifyStudyMember(Long studyId, Long userId, StudyMemberRepository studyMemberRepository) {
        boolean isMember = studyMemberRepository.existsByStudy_StudyIdAndUser_UserIdAndStatus(
                studyId, userId, StudyMemberStatus.ACTIVE
        );
        if (!isMember) {
            throw new StudyException(StudyErrorCode.STUDY_UNAUTHORIZED_ACCESS);
        }
    }

    /**
     * ACTIVE 또는 WITHDRAWN 멤버 모두를 허용합니다.
     * 탈퇴 후 개인 학습 자산(필기·퀴즈·피드백) 읽기 권한 검증에 사용합니다.
     */
    public static void verifyStudyMemberAllowWithdrawn(Long studyId, Long userId, StudyMemberRepository studyMemberRepository) {
        boolean isMember = studyMemberRepository.existsByStudy_StudyIdAndUser_UserIdAndStatusIn(
                studyId, userId, List.of(StudyMemberStatus.ACTIVE, StudyMemberStatus.WITHDRAWN)
        );
        if (!isMember) {
            throw new StudyException(StudyErrorCode.STUDY_UNAUTHORIZED_ACCESS);
        }
    }

    // 현재 사용자가 해당 스터디 멤버 본인인지 검증함 (수정/삭제 권한 등)
    public static void verifyOwnership(StudyMember studyMember, Long userId) {
        if (studyMember == null || studyMember.getUser() == null || !studyMember.getUser().getUserId().equals(userId)) {
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

