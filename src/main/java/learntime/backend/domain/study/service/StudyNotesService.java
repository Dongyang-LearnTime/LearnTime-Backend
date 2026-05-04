package learntime.backend.domain.study.service;

import learntime.backend.domain.study.dto.request.StudyNoteRequestDTO;
import learntime.backend.domain.study.error.code.StudyErrorCode;
import learntime.backend.domain.study.error.exception.StudyException;
import learntime.backend.domain.study.model.Study;
import learntime.backend.domain.study.model.StudyNotes;
import learntime.backend.domain.study.repository.StudyNotesRepository;
import learntime.backend.domain.study.repository.StudyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StudyNotesService {

    private final StudyRepository studyRepository;
    private final StudyNotesRepository studyNotesRepository;

    public void create(StudyNoteRequestDTO request) {
        Study study = studyRepository.findById(request.studyId())
                .orElseThrow(() -> new StudyException(StudyErrorCode.STUDY_NOT_FOUND));

        StudyNotes studyNotes = StudyNotes.builder()
                .study(study)
                .noteContents(request.content())
                .noteTitle(request.title())
                .build();

        studyNotesRepository.save(studyNotes);
    }

}
