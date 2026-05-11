package learntime.backend.domain.study.service.core;

import learntime.backend.domain.study.converter.StudyConverter;
import learntime.backend.domain.study.dto.request.UpdateFeedbackTitleRequestDTO;
import learntime.backend.domain.study.dto.response.*;
import learntime.backend.domain.study.error.code.StudyErrorCode;
import learntime.backend.domain.study.error.exception.StudyException;
import learntime.backend.domain.study.model.Study;
import learntime.backend.domain.study.model.StudyFeedback;
import learntime.backend.domain.study.repository.StudyFeedbackRepository;
import learntime.backend.domain.study.repository.StudyRepository;
import learntime.backend.domain.study.service.ai.GeminiFeedbackService;
import learntime.backend.global.utils.AuthorizationUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StudyFeedbackService {

    private final StudyRepository studyRepository;
    private final StudyFeedbackRepository studyFeedbackRepository;
    private final learntime.backend.domain.study.repository.StudyDailyPlanRepository studyDailyPlanRepository;
    private final StudyQueryService studyQueryService;
    private final GeminiFeedbackService geminiFeedbackService;

    // 현재 학습 지표를 종합하여 AI 피드백을 생성하고 데이터베이스에 저장합니다.
    @Transactional
    public StudyFeedbackResponseDTO generateAndSaveFeedback(Long studyId, Long userId) {
        Study study = studyRepository.findById(studyId)
                .orElseThrow(() -> new StudyException(StudyErrorCode.STUDY_NOT_FOUND));

        AuthorizationUtil.verifyOwnership(userId, study.getUser().getUserId());

        StudyTotalInfoResponseDTO indicator = studyQueryService.getStudyTotalIndicator(studyId);
        
        List<learntime.backend.domain.study.model.StudyDailyPlan> completedPlans = 
                studyDailyPlanRepository.findCompletedPlansByStudyId(studyId);
                
        List<StudyAnalysisDataDTO.DailyTopicStats> topicStats = completedPlans.stream()
                .map(plan -> StudyAnalysisDataDTO.DailyTopicStats.builder()
                        .topicContent(plan.getPlanContent())
                        .completionStatus(plan.getCompletionStatus() != null ? plan.getCompletionStatus().name() : "알 수 없음")
                        .understandingScore(plan.getUnderstandingScore())
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
                .study(study)
                .feedbackTitle(aiResponse.feedbackTitle())
                .feedbackContent(aiResponse.feedbackContent())
                .build();

        StudyFeedback savedFeedback = studyFeedbackRepository.save(feedback);

        return StudyConverter.toStudyFeedbackResponseDTO(savedFeedback);
    }

    // 특정 스터디에 대해 생성된 AI 피드백 목록을 최신순으로 조회합니다.
    @Transactional(readOnly = true)
    public List<StudyFeedbackResponseDTO> getFeedbackList(Long studyId, Long userId) {
        Study study = studyRepository.findById(studyId)
                .orElseThrow(() -> new StudyException(StudyErrorCode.STUDY_NOT_FOUND));

        AuthorizationUtil.verifyOwnership(userId, study.getUser().getUserId());

        List<StudyFeedback> feedbacks = studyFeedbackRepository.findAllByStudy_StudyIdOrderByCreatedAtDesc(studyId);

        return feedbacks.stream()
                .map(StudyConverter::toStudyFeedbackResponseDTO)
                .toList();
    }

    // 사용자가 AI가 생성한 피드백의 제목을 직접 수정합니다.
    @Transactional
    public void updateFeedbackTitle(UpdateFeedbackTitleRequestDTO request, Long userId) {
        StudyFeedback feedback = studyFeedbackRepository.findById(request.feedbackId())
                .orElseThrow(() -> new IllegalArgumentException("해당 피드백을 찾을 수 없습니다."));

        AuthorizationUtil.verifyOwnership(userId, feedback.getStudy().getUser().getUserId());

        feedback.updateTitle(request.feedbackTitle());
    }

    // 저장된 특정 AI 피드백을 삭제합니다.
    @Transactional
    public void deleteFeedback(Long feedbackId, Long userId) {
        StudyFeedback feedback = studyFeedbackRepository.findById(feedbackId)
                .orElseThrow(() -> new IllegalArgumentException("해당 피드백을 찾을 수 없습니다."));

        AuthorizationUtil.verifyOwnership(userId, feedback.getStudy().getUser().getUserId());

        studyFeedbackRepository.delete(feedback);
    }
}
