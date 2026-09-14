package learntime.backend.domain.study.service.core;

import learntime.backend.domain.study.dto.request.UpdateStudyTitleRequestDTO;
import learntime.backend.domain.study_member.enums.StudyMemberStatus;
import learntime.backend.domain.study.error.code.StudyErrorCode;
import learntime.backend.domain.study.error.exception.StudyException;
import learntime.backend.domain.study.model.Study;
import learntime.backend.domain.study_member.model.StudyMember;
import learntime.backend.domain.study_member.repository.StudyMemberRepository;
import learntime.backend.domain.study.repository.StudyRepository;
import learntime.backend.global.utils.StudyAuthUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// 스터디 생성, 재생성, 초기화 등 상태를 변경하는 로직 담당 서비스
@Slf4j
@Service
@RequiredArgsConstructor
public class StudyManagementService {

    private final StudyRepository studyRepository;
    private final StudyMemberRepository studyMemberRepository;

    @Transactional
    public void updateTitle(UpdateStudyTitleRequestDTO request, Long userId, boolean isStudyTitle) {
        Study study = studyRepository.findById(request.studyId())
                .orElseThrow(() -> new StudyException(StudyErrorCode.STUDY_NOT_FOUND));

        StudyMember studyMember = studyMemberRepository.findByStudy_StudyIdAndUser_UserIdAndStatusIn(
                        request.studyId(),
                        userId,
                        List.of(StudyMemberStatus.ACTIVE, StudyMemberStatus.COMPLETED)
                )
                .orElseThrow(() -> new StudyException(StudyErrorCode.STUDY_MEMBER_NOT_FOUND));

        if (studyMember.getStatus() == StudyMemberStatus.COMPLETED) {
            throw new StudyException(StudyErrorCode.STUDY_MEMBER_ALREADY_COMPLETED);
        }

        // 방장 권한 검증
        StudyAuthUtil.checkOwnerRole(studyMember);

        // 공부 진도 제목인지, 책 제목인지 확인
        if (isStudyTitle) {
            study.updateStudyTitle(request.title());
        } else {
            study.updateBookTitle(request.title());
        }
    }

    @Transactional
    public void updateVisibility(Long studyId, Boolean isPublic, Long userId) {
        Study study = studyRepository.findById(studyId)
                .orElseThrow(() -> new StudyException(StudyErrorCode.STUDY_NOT_FOUND));

        StudyMember studyMember = studyMemberRepository.findByStudy_StudyIdAndUser_UserIdAndStatusIn(
                        studyId,
                        userId,
                        List.of(StudyMemberStatus.ACTIVE, StudyMemberStatus.COMPLETED)
                )
                .orElseThrow(() -> new StudyException(StudyErrorCode.STUDY_MEMBER_NOT_FOUND));

        if (studyMember.getStatus() == StudyMemberStatus.COMPLETED) {
            throw new StudyException(StudyErrorCode.STUDY_MEMBER_ALREADY_COMPLETED);
        }

        // 방장 권한 검증
        StudyAuthUtil.checkOwnerRole(studyMember);

        study.updateVisibility(isPublic);
        log.info("[Study Visibility] 스터디 ID: {}, 공개 여부 변경: {}, 변경자: {}", studyId, isPublic, userId);
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

        StudyMember studyMember = studyMemberRepository.findByStudy_StudyIdAndUser_UserIdAndStatusIn(
                        studyId,
                        userId,
                        java.util.List.of(StudyMemberStatus.ACTIVE, StudyMemberStatus.COMPLETED)
                )
                .orElseThrow(() -> new StudyException(StudyErrorCode.STUDY_MEMBER_NOT_FOUND));

        // 방장 권한 검증
        StudyAuthUtil.checkOwnerRole(studyMember);

        // 1. 가장 하위 계층(1계층) 벌크 삭제
        studyRepository.deleteStudyMemberContentsByStudyId(studyId);
        studyRepository.deleteQuizAnswersByStudyId(studyId);
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
        studyRepository.deleteStudyJoinRequestsByStudyId(studyId);

        // 4. StudyMember 및 Study 본체 삭제
        studyRepository.deleteStudyMembersByStudyId(studyId);
        studyRepository.deleteStudyById(studyId);

        log.info("[Study Delete] 스터디 벌크 삭제 완료 - 스터디 ID: {}, 요청자 ID: {}", studyId, userId);
    }
}
