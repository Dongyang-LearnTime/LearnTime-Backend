package learntime.backend.domain.study.converter;

import learntime.backend.domain.study.model.Study;
import learntime.backend.domain.study.model.StudyNotes;
import learntime.backend.global.error.code.ErrorCode;
import learntime.backend.global.error.exception.BusinessException;

public class StudyNotesConverter {

    public StudyNotesConverter() {
        throw new BusinessException(ErrorCode.UTILITY_CLASS_INSTANTIATION);
    }

    public static StudyNotes toStudyNotesEntity(Study study, String title, String content) {
        return StudyNotes.builder()
                .study(study)
                .noteTitle(title)
                .noteContents(content)
                .build();
    }
}
