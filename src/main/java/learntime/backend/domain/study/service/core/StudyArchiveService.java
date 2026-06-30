package learntime.backend.domain.study.service.core;

import learntime.backend.domain.study.converter.StudyArchiveConverter;
import learntime.backend.domain.study.dto.response.StudyArchiveResponseDTO;
import learntime.backend.domain.study_feedback.repository.StudyFeedbackRepository;
import learntime.backend.domain.study_member.enums.StudyMemberStatus;
import learntime.backend.domain.study_member.model.StudyMember;
import learntime.backend.domain.study_member.repository.StudyMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


@Service
@RequiredArgsConstructor
public class StudyArchiveService {

    private final StudyMemberRepository studyMemberRepository;
    private final StudyFeedbackRepository studyFeedbackRepository;


    @Transactional(readOnly = true)
    public List<StudyArchiveResponseDTO> getMyArchivedStudies(Long userId) {
        List<StudyMember> allMemberships = studyMemberRepository.findAllByUserIdAndStatusIn(
                userId,
                List.of(StudyMemberStatus.ACTIVE, StudyMemberStatus.WITHDRAWN)
        );
        return allMemberships.stream()
                .map(StudyArchiveConverter::toStudyArchiveResponseDTO)
                .toList();
    }

}
