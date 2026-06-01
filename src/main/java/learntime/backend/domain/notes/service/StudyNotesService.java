package learntime.backend.domain.notes.service;

import learntime.backend.domain.notes.dto.request.StudyNoteRequestDTO;
import learntime.backend.domain.notes.dto.request.StudyNotesUpdateRequestDTO;
import learntime.backend.domain.notes.dto.response.StudyNotesResponseDTO;
import learntime.backend.domain.study.error.code.StudyErrorCode;
import learntime.backend.domain.study.error.exception.StudyException;
import learntime.backend.domain.notes.model.StudyNotes;
import learntime.backend.domain.notes.converter.StudyNotesConverter;
import learntime.backend.domain.notes.repository.StudyNotesRepository;
import learntime.backend.domain.study_member.enums.StudyMemberStatus;
import learntime.backend.domain.study_member.model.StudyMember;
import learntime.backend.domain.study_member.repository.StudyMemberRepository;
import learntime.backend.global.utils.StudyAuthUtil;
import learntime.backend.global.dto.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StudyNotesService {

    private final StudyMemberRepository studyMemberRepository;
    private final StudyNotesRepository studyNotesRepository;

    @Transactional(readOnly = true)
    public StudyNotesResponseDTO getNote(Long studyNotesId, Long userId) {
        StudyNotes studyNotes = findByNotesId(studyNotesId);
        // 본인만 본인의 필기를 조회할 수 있음
        StudyAuthUtil.verifyOwnership(studyNotes.getStudyMember(), userId);

        return StudyNotesConverter.toStudyNotesResponseDTO(studyNotes);
    }

    @Transactional(readOnly = true)
    public PageResponse<StudyNotesResponseDTO> getNotesList(Long studyId, Pageable pageable, Long userId) {
        StudyMember studyMember = findByStudyIdAndUserId(studyId, userId);
        // 탈퇴(WITHDRAWN) 멤버도 자신의 필기 목록을 조회할 수 있음
        StudyAuthUtil.verifyStudyMemberAllowWithdrawn(studyId, userId, studyMemberRepository);

        // StudyMember 기준으로 필기 목록 가져옴
        Page<StudyNotes> notes = studyNotesRepository.findByStudyMember(studyMember, pageable);

        return PageResponse.of(notes.map(StudyNotesConverter::toStudyNotesResponseDTO));
    }

    @Transactional
    public Long create(StudyNoteRequestDTO request, Long userId) {
        StudyMember studyMember = findByStudyIdAndUserId(request.studyId(), userId);
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

    // ACTIVE + WITHDRAWN 모두 허용 — 필기는 개인 자산이므로 탈퇴 후에도 Full CRUD
    private StudyMember findByStudyIdAndUserId(Long studyId, Long userId) {
        return studyMemberRepository.findByStudy_StudyIdAndUser_UserIdAndStatusIn(
                        studyId,
                        userId,
                        List.of(StudyMemberStatus.ACTIVE, StudyMemberStatus.WITHDRAWN)
                )
                .orElseThrow(() -> new StudyException(StudyErrorCode.STUDY_MEMBER_NOT_FOUND));
    }

}
