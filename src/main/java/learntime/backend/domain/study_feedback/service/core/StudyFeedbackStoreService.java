package learntime.backend.domain.study_feedback.service.core;

import learntime.backend.domain.study.converter.StudyConverter;
import learntime.backend.domain.study_feedback.dto.response.AiFeedbackResponseDTO;
import learntime.backend.domain.study.error.code.StudyErrorCode;
import learntime.backend.domain.study.error.exception.StudyException;
import learntime.backend.domain.study_feedback.model.StudyFeedback;
import learntime.backend.domain.study_feedback.repository.StudyFeedbackRepository;
import learntime.backend.domain.study_member.enums.StudyMemberStatus;
import learntime.backend.domain.study_member.model.StudyMember;
import learntime.backend.domain.study_member.repository.StudyMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StudyFeedbackStoreService {

    private final StudyFeedbackRepository studyFeedbackRepository;
    private final StudyMemberRepository studyMemberRepository;

    @Transactional
    public StudyFeedbackResponseDTO saveGeneratedFeedback(Long studyId, Long userId, AiFeedbackResponseDTO aiResponse) {
        StudyMember member = studyMemberRepository.findByStudy_StudyIdAndUser_UserIdAndStatus(
                        studyId,
                        userId,
                        StudyMemberStatus.ACTIVE
                )
                .orElseThrow(() -> new StudyException(StudyErrorCode.STUDY_MEMBER_NOT_FOUND));

        StudyFeedback feedback = StudyFeedback.builder()
                .feedbackTitle(aiResponse.feedbackTitle())
                .feedbackContent(aiResponse.feedbackContent())
                .studyMember(member)
                .build();

        studyFeedbackRepository.save(feedback);

        return StudyConverter.toStudyFeedbackResponseDTO(feedback);
    }
}
