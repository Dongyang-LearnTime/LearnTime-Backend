package learntime.backend.domain.study.service.core;

import learntime.backend.domain.study.converter.StudyConverter;
import learntime.backend.domain.study.dto.request.UpdateFeedbackTitleRequestDTO;
import learntime.backend.domain.study.dto.response.AiFeedbackResponseDTO;
import learntime.backend.domain.study.dto.response.StudyAnalysisDataDTO;
import learntime.backend.domain.study.dto.response.StudyFeedbackResponseDTO;
import learntime.backend.domain.study.error.code.StudyErrorCode;
import learntime.backend.domain.study.error.exception.StudyException;
import learntime.backend.domain.study.model.*;
import learntime.backend.domain.study.repository.StudyFeedbackRepository;
import learntime.backend.domain.study_member.enums.StudyMemberStatus;
import learntime.backend.domain.study_member.repository.StudyMemberRepository;
import learntime.backend.domain.study.service.ai.GeminiFeedbackService;
import learntime.backend.domain.study_member.model.StudyMember;
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

    /** 사용자의 최근 학습 데이터를 바탕으로 AI 피드백을 생성하고 저장합니다. */
    @Transactional
    public StudyFeedbackResponseDTO generateAndSaveFeedback(Long studyId, Long userId) {
        StudyMember member = studyMemberRepository.findByStudy_StudyIdAndUser_UserIdAndStatus(
                        studyId,
                        userId,
                        StudyMemberStatus.ACTIVE
                )
                .orElseThrow(() -> new StudyException(StudyErrorCode.STUDY_MEMBER_NOT_FOUND));

        // 1. 피드백 생성을 위한 분석 데이터 추출
        StudyAnalysisDataDTO analysisData = studyQueryService.getStudyAnalysisData(studyId, userId);

        if (analysisData.topicStats().isEmpty()) {
            throw new StudyException(StudyErrorCode.FEEDBACK_NOT_ENOUGH_DATA);
        }

        // 2. AI 피드백 생성 요청
        AiFeedbackResponseDTO aiResponse = geminiFeedbackService.generateStudyFeedback(analysisData, member.getUser().getName(), userId);

        // 3. 결과 저장
        StudyFeedback feedback = StudyFeedback.builder()
                .feedbackTitle(aiResponse.feedbackTitle())
                .feedbackContent(aiResponse.feedbackContent())
                .studyMember(member)
                .build();

        studyFeedbackRepository.save(feedback);

        return StudyConverter.toStudyFeedbackResponseDTO(feedback);
    }

    /** 특정 스터디 멤버의 모든 피드백 기록을 조회합니다. (오프셋 페이징) */
    @Transactional(readOnly = true)
    public PageResponse<StudyFeedbackResponseDTO> getMemberFeedbacks(Long studyMemberId, Pageable pageable, Long userId) {
        Page<StudyFeedback> feedbacks = studyFeedbackRepository.findAllByStudyMember_StudyMemberId(studyMemberId, pageable);
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
