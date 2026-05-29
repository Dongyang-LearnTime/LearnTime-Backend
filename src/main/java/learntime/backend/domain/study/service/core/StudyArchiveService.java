package learntime.backend.domain.study.service.core;

import learntime.backend.domain.study.converter.StudyArchiveConverter;
import learntime.backend.domain.study.converter.StudyConverter;
import learntime.backend.domain.study.dto.response.StudyArchiveResponseDTO;
import learntime.backend.domain.study.dto.response.StudyFeedbackResponseDTO;
import learntime.backend.domain.study.error.code.StudyErrorCode;
import learntime.backend.domain.study.error.exception.StudyException;
import learntime.backend.domain.study.model.StudyFeedback;
import learntime.backend.domain.study.repository.StudyFeedbackRepository;
import learntime.backend.domain.study_member.enums.StudyMemberStatus;
import learntime.backend.domain.study_member.model.StudyMember;
import learntime.backend.domain.study_member.repository.StudyMemberRepository;
import learntime.backend.global.dto.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
