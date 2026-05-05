package learntime.backend.domain.study.converter;

import learntime.backend.domain.study.model.Study;
import learntime.backend.domain.study.model.StudyNotes;

public class StudyNotesConverter {

    public StudyNotesConverter() { }

    public static StudyNotes toStudyNotesEntity(Study study, String title, String content) {
        return StudyNotes.builder()
                .study(study)
                .noteTitle(title)
                .noteContents(content)
                .build();
    }
}
