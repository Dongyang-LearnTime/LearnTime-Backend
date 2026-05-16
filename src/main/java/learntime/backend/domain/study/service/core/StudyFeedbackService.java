package learntime.backend.domain.study.service.core;

import learntime.backend.domain.study.converter.StudyConverter;
import learntime.backend.domain.study.dto.request.UpdateFeedbackTitleRequestDTO;
import learntime.backend.domain.study.dto.response.*;
import learntime.backend.domain.study.error.code.StudyErrorCode;
import learntime.backend.domain.study.error.exception.StudyException;
import learntime.backend.domain.study.model.StudyFeedback;
import learntime.backend.domain.study.model.StudyMember;
import learntime.backend.domain.study.model.StudyStatus;
import learntime.backend.domain.study.repository.StudyFeedbackRepository;
import learntime.backend.domain.study.repository.StudyMemberRepository;
import learntime.backend.domain.study.repository.StudyStatusRepository;
import learntime.backend.domain.study.service.ai.GeminiFeedbackService;
import learntime.backend.domain.study.service.util.StudyAuthUtil;
import learntime.backend.global.utils.AuthorizationUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StudyFeedbackService {

    private final StudyMemberRepository studyMemberRepository;
    private final StudyFeedbackRepository studyFeedbackRepository;
    private final StudyStatusRepository studyStatusRepository;
    private final StudyQueryService studyQueryService;
    private final GeminiFeedbackService geminiFeedbackService;

    // 현재 학습 지표를 종합하여 AI 피드백을 생성하고 데이터베이스에 저장한다.
    @Transactional
    public StudyFeedbackResponseDTO generateAndSaveFeedback(Long studyMemberId, Long userId) {
        StudyMember studyMember = studyMemberRepository.findById(studyMemberId)
                .orElseThrow(() -> new StudyException(StudyErrorCode.STUDY_MEMBER_NOT_FOUND));

        AuthorizationUtil.verifyOwnership(userId, studyMember.getUser().getUserId());

        StudyTotalInfoResponseDTO indicator = studyQueryService.
                getStudyMemberTotalIndicatorByUserId(studyMember.getStudy().getStudyId(), userId);
        
        List<StudyStatus> completedStatuses =
                studyStatusRepository.findCompletedStatusByStudyMemberId(studyMemberId);
                
        List<StudyAnalysisDataDTO.DailyTopicStats> topicStats = completedStatuses.stream()
                .map(status -> StudyAnalysisDataDTO.DailyTopicStats.builder()
                        .topicContent(status.getStudyDailyPlan().getPlanContent())
                        .completionStatus(status.getCompletionStatus() != null ? status.getCompletionStatus().name() : "알 수 없음")
                        .understandingScore(status.getUnderstandingScore())
                        .build())
                .toList();
                
        StudyAnalysisDataDTO analysisData = StudyAnalysisDataDTO.builder()
                .studyCompletionRate(indicator.studyCompletionRate())
                .studySuccessRate(indicator.studySuccessRate())
                .totalFocusedTime(indicator.totalFocusedTime())
                .topicStats(topicStats)
                .build();

        AiFeedbackResponseDTO aiResponse = geminiFeedbackService.generateFeedback(analysisData, userId);

        StudyFeedback feedback = StudyFeedback.builder()
                .studyMember(studyMember)
                .feedbackTitle(aiResponse.feedbackTitle())
                .feedbackContent(aiResponse.feedbackContent())
                .build();

        StudyFeedback savedFeedback = studyFeedbackRepository.save(feedback);

        return StudyConverter.toStudyFeedbackResponseDTO(savedFeedback);
    }

    // 특정 스터디에 대해 생성된 AI 피드백 목록을 최신순으로 조회한다.
    @Transactional(readOnly = true)
    public List<StudyFeedbackResponseDTO> getFeedbackList(Long studyMemberId, Long userId) {
        StudyMember studyMember = studyMemberRepository.findById(studyMemberId)
                .orElseThrow(() -> new StudyException(StudyErrorCode.STUDY_MEMBER_NOT_FOUND));

        AuthorizationUtil.verifyOwnership(userId, studyMember.getUser().getUserId());

        List<StudyFeedback> feedbacks =
                studyFeedbackRepository
                        .findAllByStudyMember_StudyMemberIdOrderByCreatedAtDesc(studyMemberId);

        return feedbacks.stream()
                .map(StudyConverter::toStudyFeedbackResponseDTO)
                .toList();
    }

    // 사용자가 AI가 생성한 피드백의 제목을 직접 수정한다.
    @Transactional
    public void updateFeedbackTitle(UpdateFeedbackTitleRequestDTO request, Long userId) {
        StudyFeedback feedback = studyFeedbackRepository.findById(request.feedbackId())
                .orElseThrow(() -> new IllegalArgumentException("해당 피드백을 찾을 수 없습니다."));

        AuthorizationUtil.verifyOwnership(userId, feedback.getStudyMember().getUser().getUserId());

        feedback.updateTitle(request.feedbackTitle());
    }

    // 특정 AI 피드백을 삭제함
    @Transactional
    public void deleteFeedback(Long feedbackId, Long userId) {
        StudyFeedback feedback = studyFeedbackRepository.findById(feedbackId)
                .orElseThrow(() -> new IllegalArgumentException("해당 피드백을 찾을 수 없습니다."));

        // 본인만 삭제할 수 있음
        StudyAuthUtil.verifyOwnership(feedback.getStudyMember(), userId);

        studyFeedbackRepository.delete(feedback);
    }
}
