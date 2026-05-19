package learntime.backend.domain.studymember.service;

import learntime.backend.domain.studymember.converter.StudyMemberConverter;
import learntime.backend.domain.studymember.dto.response.StudyMemberResponseDTO;
import learntime.backend.domain.studymember.enums.StudyMemberRole;
import learntime.backend.domain.study.error.code.StudyErrorCode;
import learntime.backend.domain.study.error.exception.StudyException;
import learntime.backend.domain.studymember.dto.request.ChangeOwnerRequestDTO;
import learntime.backend.domain.studymember.model.StudyMember;
import learntime.backend.domain.studymember.repository.StudyMemberRepository;
import learntime.backend.global.utils.StudyAuthUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@RequiredArgsConstructor
@Service
public class StudyMemberService {

    private final StudyMemberRepository studyMemberRepository;

    @Transactional(readOnly = true)
    public List<StudyMemberResponseDTO> getAllStudyMember(Long studyId, Long userId) {
        boolean isStudyMember =
                studyMemberRepository.existsByStudy_StudyIdAndUser_UserId(studyId, userId);
        if (!isStudyMember) {
            throw new StudyException(StudyErrorCode.STUDY_MEMBER_NOT_FOUND);
        }

        List<StudyMember> studyMemberList = studyMemberRepository.findAllByStudyIdFetchUser(studyId);
        return studyMemberList.stream()
                .map(StudyMemberConverter::toStudyMemberResponse)
                .toList();
    }

    @Transactional
    public void changeStudyOwner(ChangeOwnerRequestDTO request, Long ownerId) {
        // 현재 OWNER 멤버 조회
        StudyMember ownerStudyMember = studyMemberRepository
                .findByStudy_StudyIdAndUser_UserId(request.studyId(), ownerId)
                .orElseThrow(() -> new StudyException(StudyErrorCode.STUDY_MEMBER_NOT_FOUND));

        // 새 OWNER 멤버 조회
        StudyMember newOwnerStudyMember = studyMemberRepository.findById(request.newOwnerMemberId())
                .orElseThrow(() -> new StudyException(StudyErrorCode.STUDY_MEMBER_NOT_FOUND));

        // 방장과 바뀔 맴버가 동일 인물인지 확인
        if (ownerId.equals(newOwnerStudyMember.getUser().getUserId())) {
            throw new StudyException(StudyErrorCode.INVALID_OWNER_TRANSFER);
        }

        StudyAuthUtil.checkOwnerRole(ownerStudyMember); // 요청자가 방장인지 확인

        ownerStudyMember.changeRole(StudyMemberRole.MEMBER);
        newOwnerStudyMember.changeRole(StudyMemberRole.OWNER);
    }

}
