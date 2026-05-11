package learntime.backend.domain.notes.service;

import learntime.backend.domain.notes.dto.request.StudyNoteRequestDTO;
import learntime.backend.domain.notes.dto.request.StudyNotesUpdateRequestDTO;
import learntime.backend.domain.notes.dto.response.StudyNotesResponseDTO;
import learntime.backend.domain.study.error.code.StudyErrorCode;
import learntime.backend.domain.study.error.exception.StudyException;
import learntime.backend.domain.study.model.Study;
import learntime.backend.domain.notes.model.StudyNotes;
import learntime.backend.domain.notes.converter.StudyNotesConverter;
import learntime.backend.domain.notes.repository.StudyNotesRepository;
import learntime.backend.domain.study.repository.StudyRepository;
import learntime.backend.global.utils.AuthorizationUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

// 학습 필기(Notes) CRUD 비즈니스 로직 담당 서비스
@Service
@RequiredArgsConstructor
public class StudyNotesService {

    private final StudyRepository studyRepository;
    private final StudyNotesRepository studyNotesRepository;

    @Transactional(readOnly = true)
    public StudyNotesResponseDTO getNote(Long studyNotesId, Long userId) {
        StudyNotes studyNotes = studyNotesRepository.findById(studyNotesId)
                .orElseThrow(() -> new StudyException(StudyErrorCode.STUDY_NOTE_NOT_FOUND));

        AuthorizationUtil.verifyOwnership(userId, studyNotes.getStudy().getUser().getUserId());

        return StudyNotesResponseDTO.from(studyNotes);
    }

    @Transactional(readOnly = true)
    public List<StudyNotesResponseDTO> getNotesByStudyId(Long studyId, Long userId) {
        Study study = studyRepository.findById(studyId)
                .orElseThrow(() -> new StudyException(StudyErrorCode.STUDY_NOT_FOUND));
                
        AuthorizationUtil.verifyOwnership(userId, study.getUser().getUserId());
        
        List<StudyNotes> notes = studyNotesRepository.findByStudy_StudyId(studyId);
        return notes.stream()
                .map(StudyNotesResponseDTO::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public Long create(StudyNoteRequestDTO request, Long userId) {
        Study study = studyRepository.findById(request.studyId())
                .orElseThrow(() -> new StudyException(StudyErrorCode.STUDY_NOT_FOUND));

        AuthorizationUtil.verifyOwnership(userId, study.getUser().getUserId());

        StudyNotes studyNotes = StudyNotesConverter.toStudyNotesEntity(study, request.title(), request.content());

        StudyNotes saveNotes = studyNotesRepository.save(studyNotes);

        return saveNotes.getStudyNotesId();
    }

    @Transactional
    public void update(Long studyNotesId, StudyNotesUpdateRequestDTO request, Long userId) {
        StudyNotes studyNotes = studyNotesRepository.findById(studyNotesId)
                .orElseThrow(() -> new StudyException(StudyErrorCode.STUDY_NOTE_NOT_FOUND));
                
        AuthorizationUtil.verifyOwnership(userId, studyNotes.getStudy().getUser().getUserId());
                
        studyNotes.update(request.title(), request.content());
    }

    @Transactional
    public void delete(Long studyNotesId, Long userId) {
        StudyNotes studyNotes = studyNotesRepository.findById(studyNotesId)
                .orElseThrow(() -> new StudyException(StudyErrorCode.STUDY_NOTE_NOT_FOUND));
                
        AuthorizationUtil.verifyOwnership(userId, studyNotes.getStudy().getUser().getUserId());
                
        studyNotesRepository.delete(studyNotes);
    }
}
