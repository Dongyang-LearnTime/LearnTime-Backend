package learntime.backend.domain.notes.converter;

import learntime.backend.domain.notes.dto.response.StudyNotesResponseDTO;
import learntime.backend.domain.notes.model.StudyNotes;
import learntime.backend.domain.study.model.StudyMember;
import learntime.backend.global.error.code.ErrorCode;
import learntime.backend.global.error.exception.BusinessException;

public class StudyNotesConverter {

    public StudyNotesConverter() {
        throw new BusinessException(ErrorCode.UTILITY_CLASS_INSTANTIATION);
    }

    public static StudyNotes toStudyNotesEntity(StudyMember studyMember, String title, String content) {
        return StudyNotes.builder()
                .studyMember(studyMember)
                .noteTitle(title)
                .noteContents(content)
                .build();
    }

    public static StudyNotesResponseDTO toStudyNotesResponseDTO(StudyNotes studyNotes) {
        return StudyNotesResponseDTO.builder()
                .studyNotesId(studyNotes.getStudyNotesId())
                .studyId(studyNotes.getStudyMember() != null ? studyNotes.getStudyMember().getStudy().getStudyId() : null)
                .studyMemberId(studyNotes.getStudyMember() != null ? studyNotes.getStudyMember().getStudyMemberId() : null)
                .title(studyNotes.getNoteTitle())
                .content(studyNotes.getNoteContents())
                .createdAt(studyNotes.getCreatedAt())
                .updatedAt(studyNotes.getUpdatedAt())
                .build();
    }

}
