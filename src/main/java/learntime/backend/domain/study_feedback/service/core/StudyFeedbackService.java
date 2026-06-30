package learntime.backend.domain.study_feedback.service.core;

import learntime.backend.domain.study.converter.StudyConverter;
import learntime.backend.domain.study_feedback.dto.request.UpdateFeedbackTitleRequestDTO;
import learntime.backend.domain.study_feedback.dto.response.AiFeedbackResponseDTO;
import learntime.backend.domain.study_feedback.model.StudyFeedback;
import learntime.backend.domain.study_progress.dto.response.StudyAnalysisDataDTO;
import learntime.backend.domain.study.error.code.StudyErrorCode;
import learntime.backend.domain.study.error.exception.StudyException;
import learntime.backend.domain.study_feedback.repository.StudyFeedbackRepository;
import learntime.backend.domain.study_member.enums.StudyMemberStatus;
import learntime.backend.domain.study_member.repository.StudyMemberRepository;
import learntime.backend.domain.study_feedback.service.ai.GeminiFeedbackService;
import learntime.backend.domain.study_member.model.StudyMember;
import learntime.backend.domain.study_progress.service.StudyQueryService;
import learntime.backend.global.dto.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// AI를 활용한 학습 피드백 생성 및 관리 서비스
@Service
@RequiredArgsConstructor
public class StudyFeedbackService {

    private final GeminiFeedbackService geminiFeedbackService;
    private final StudyFeedbackRepository studyFeedbackRepository;
    private final StudyMemberRepository studyMemberRepository;
    private final StudyQueryService studyQueryService;
    private final StudyFeedbackStoreService studyFeedbackStoreService;

    /** 사용자의 최근 학습 데이터를 바탕으로 AI 피드백을 생성하고 저장합니다. */
    public StudyFeedbackResponseDTO generateAndSaveFeedback(Long studyId, Long userId) {
        // 1. 피드백 생성을 위한 분석 데이터 추출
        StudyAnalysisDataDTO analysisData = studyQueryService.getStudyAnalysisData(studyId, userId);

        if (analysisData.topicStats().isEmpty()) {
            throw new StudyException(StudyErrorCode.FEEDBACK_NOT_ENOUGH_DATA);
        }

        // 2. AI 피드백 생성 요청 (외부 API 호출 - 트랜잭션 없음)
        AiFeedbackResponseDTO aiResponse = geminiFeedbackService.generateStudyFeedback(analysisData, userId);

        // 3. 결과 저장 (새로운 트랜잭션에서 수행)
        return studyFeedbackStoreService.saveGeneratedFeedback(studyId, userId, aiResponse);
    }

    /** 특정 스터디 멤버의 모든 피드백 기록을 조회합니다. (오프셋 페이징)
     * 탈퇴(WITHDRAWN) 멤버도 자신의 과거 피드백을 조회할 수 있습니다. */
    @Transactional(readOnly = true)
    public PageResponse<StudyFeedbackResponseDTO> getMemberFeedbacks(Long studyId, Pageable pageable, Long userId) {
        StudyMember member = studyMemberRepository.findByStudy_StudyIdAndUser_UserIdAndStatusIn(
                        studyId,
                        userId,
                        List.of(StudyMemberStatus.ACTIVE, StudyMemberStatus.WITHDRAWN, StudyMemberStatus.COMPLETED)
                )
                .orElseThrow(() -> new StudyException(StudyErrorCode.STUDY_MEMBER_NOT_FOUND));

        Page<StudyFeedback> feedbacks = studyFeedbackRepository.findAllByStudyMember_StudyMemberId(member.getStudyMemberId(), pageable);
        return PageResponse.of(feedbacks.map(StudyConverter::toStudyFeedbackResponseDTO));
    }

    /** 피드백 제목을 수정합니다. */
    @Transactional
    public void updateFeedbackTitle(UpdateFeedbackTitleRequestDTO request, Long userId) {
        StudyFeedback feedback = studyFeedbackRepository.findById(request.feedbackId())
                .orElseThrow(() -> new StudyException(StudyErrorCode.FEEDBACK_NOT_FOUND));
        
        // 권한 체크 (본인의 피드백인지)
        if (!feedback.getStudyMember().getUser().getUserId().equals(userId)) {
            throw new StudyException(StudyErrorCode.STUDY_UNAUTHORIZED_ACCESS);
        }

        feedback.updateTitle(request.feedbackTitle());
    }

    /** 피드백을 삭제합니다. */
    @Transactional
    public void deleteFeedback(Long feedbackId, Long userId) {
        StudyFeedback feedback = studyFeedbackRepository.findById(feedbackId)
                .orElseThrow(() -> new StudyException(StudyErrorCode.FEEDBACK_NOT_FOUND));

        if (!feedback.getStudyMember().getUser().getUserId().equals(userId)) {
            throw new StudyException(StudyErrorCode.STUDY_UNAUTHORIZED_ACCESS);
        }

        studyFeedbackRepository.delete(feedback);
    }
}
