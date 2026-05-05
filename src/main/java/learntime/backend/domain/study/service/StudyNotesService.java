package learntime.backend.domain.study.service;

import learntime.backend.domain.study.dto.request.StudyNoteRequestDTO;
import learntime.backend.domain.study.dto.request.StudyNotesUpdateRequestDTO;
import learntime.backend.domain.study.dto.response.StudyNotesResponseDTO;
import learntime.backend.domain.study.error.code.StudyErrorCode;
import learntime.backend.domain.study.error.exception.StudyException;
import learntime.backend.domain.study.model.Study;
import learntime.backend.domain.study.model.StudyNotes;
import learntime.backend.domain.study.repository.StudyNotesRepository;
import learntime.backend.domain.study.repository.StudyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StudyNotesService {

    private final StudyRepository studyRepository;
    private final StudyNotesRepository studyNotesRepository;

    private void validateOwnership(Study study, Long userId) {
        if (study == null || !study.getUser().getUserId().equals(userId)) {
            throw new StudyException(StudyErrorCode.STUDY_UNAUTHORIZED_ACCESS);
        }
    }

    // 필기 상세 조회
    @Transactional(readOnly = true)
    public StudyNotesResponseDTO getNote(Long studyNotesId, Long userId) {
        StudyNotes studyNotes = studyNotesRepository.findById(studyNotesId)
                .orElseThrow(() -> new StudyException(StudyErrorCode.STUDY_NOTE_NOT_FOUND));

        validateOwnership(studyNotes.getStudy(), userId);

        return StudyNotesResponseDTO.from(studyNotes);
    }

    // 특정 Study에 속한 모든 필기 조회
    @Transactional(readOnly = true)
    public List<StudyNotesResponseDTO> getNotesByStudyId(Long studyId, Long userId) {
        Study study = studyRepository.findById(studyId)
                .orElseThrow(() -> new StudyException(StudyErrorCode.STUDY_NOT_FOUND));
                
        validateOwnership(study, userId);
        
        List<StudyNotes> notes = studyNotesRepository.findByStudy_StudyId(studyId);
        return notes.stream()
                .map(StudyNotesResponseDTO::from)
                .collect(Collectors.toList());
    }

    // 필기 생성
    @Transactional
    public Long create(StudyNoteRequestDTO request, Long userId) {
        Study study = studyRepository.findById(request.studyId())
                .orElseThrow(() -> new StudyException(StudyErrorCode.STUDY_NOT_FOUND));

        validateOwnership(study, userId);

        StudyNotes studyNotes = StudyNotes.builder()
                .study(study)
                .noteContents(request.content())
                .noteTitle(request.title())
                .build();

        StudyNotes saveNotes = studyNotesRepository.save(studyNotes);

        return saveNotes.getStudyNotesId();
    }

    // 필기 수정
    @Transactional
    public void update(Long studyNotesId, StudyNotesUpdateRequestDTO request, Long userId) {
        StudyNotes studyNotes = studyNotesRepository.findById(studyNotesId)
                .orElseThrow(() -> new StudyException(StudyErrorCode.STUDY_NOTE_NOT_FOUND));
                
        validateOwnership(studyNotes.getStudy(), userId);
                
        studyNotes.update(request.title(), request.content());
    }

    // 필기 삭제
    @Transactional
    public void delete(Long studyNotesId, Long userId) {
        StudyNotes studyNotes = studyNotesRepository.findById(studyNotesId)
                .orElseThrow(() -> new StudyException(StudyErrorCode.STUDY_NOTE_NOT_FOUND));
                
        validateOwnership(studyNotes.getStudy(), userId);
                
        studyNotesRepository.delete(studyNotes);
    }
}
