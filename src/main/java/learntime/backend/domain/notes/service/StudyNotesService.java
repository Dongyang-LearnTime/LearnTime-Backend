package learntime.backend.domain.notes.service;

import learntime.backend.domain.notes.dto.request.StudyNoteRequestDTO;
import learntime.backend.domain.notes.dto.request.StudyNotesUpdateRequestDTO;
import learntime.backend.domain.notes.dto.response.StudyNotesResponseDTO;
import learntime.backend.domain.study.error.code.StudyErrorCode;
import learntime.backend.domain.study.error.exception.StudyException;
import learntime.backend.domain.notes.model.StudyNotes;
import learntime.backend.domain.notes.converter.StudyNotesConverter;
import learntime.backend.domain.notes.repository.StudyNotesRepository;
import learntime.backend.domain.study.model.StudyMember;
import learntime.backend.domain.study.repository.StudyMemberRepository;
import learntime.backend.domain.study.service.util.StudyAuthUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StudyNotesService {

    private final StudyMemberRepository studyMemberRepository;
    private final StudyNotesRepository studyNotesRepository;

    @Transactional(readOnly = true)
    public StudyNotesResponseDTO getNote(Long studyNotesId, Long userId) {
        StudyNotes studyNotes = findByNotesId(studyNotesId);
        // 스터디 멤버이면 해당 스터디의 필기를 조회할 수 있음
        StudyAuthUtil.verifyStudyMember(studyNotes.getStudyMember().getStudy(), userId);

        return StudyNotesConverter.toStudyNotesResponseDTO(studyNotes);
    }

    @Transactional(readOnly = true)
    public List<StudyNotesResponseDTO> getNotesList(Long studyMemberId, Long userId) {
        StudyMember studyMember = findByStudyMemberId(studyMemberId);
        // 스터디 멤버이면 해당 스터디의 필기 목록을 조회할 수 있음
        StudyAuthUtil.verifyStudyMember(studyMember.getStudy(), userId);

        // StudyMember 기준으로 필기 목록 가져옴
        List<StudyNotes> notes = studyNotesRepository.findByStudyMember(studyMember);

        return notes.stream()
                .map(StudyNotesConverter::toStudyNotesResponseDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public Long create(StudyNoteRequestDTO request, Long userId) {
        StudyMember studyMember = findByStudyMemberId(request.studyMemberId());
        // 본인의 필기만 생성할 수 있음
        StudyAuthUtil.verifyOwnership(studyMember, userId);

        StudyNotes studyNotes = StudyNotesConverter.toStudyNotesEntity(studyMember, request.title(), request.content());
        StudyNotes saveNotes = studyNotesRepository.save(studyNotes);
        return saveNotes.getStudyNotesId();
    }

    @Transactional
    public void update(Long studyNotesId, StudyNotesUpdateRequestDTO request, Long userId) {
        StudyNotes studyNotes = findByNotesId(studyNotesId);
        // 본인의 필기만 수정할 수 있음
        StudyAuthUtil.verifyOwnership(studyNotes.getStudyMember(), userId);
                
        studyNotes.update(request.title(), request.content());
    }

    @Transactional
    public void delete(Long studyNotesId, Long userId) {
        StudyNotes studyNotes = findByNotesId(studyNotesId);
        // 본인의 필기만 삭제할 수 있음
        StudyAuthUtil.verifyOwnership(studyNotes.getStudyMember(), userId);
                
        studyNotesRepository.delete(studyNotes);
    }

    // ====== 헬퍼 메서드 ======
    private StudyNotes findByNotesId(Long studyNotesId) {
        return studyNotesRepository.findById(studyNotesId)
                .orElseThrow(() -> new StudyException(StudyErrorCode.STUDY_NOTE_NOT_FOUND));
    }

    private StudyMember findByStudyMemberId(Long studyMemberId) {
        return studyMemberRepository.findById(studyMemberId)
                .orElseThrow(() -> new StudyException(StudyErrorCode.STUDY_MEMBER_NOT_FOUND));
    }

}
