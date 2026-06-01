package learntime.backend.domain.study_member.service;

import learntime.backend.domain.relationship.repository.UserBlockRepository;
import learntime.backend.domain.study_member.converter.StudyMemberConverter;
import learntime.backend.domain.study_member.dto.response.StudyMemberResponseDTO;
import learntime.backend.domain.study_member.enums.StudyMemberRole;
import learntime.backend.domain.study_member.enums.StudyMemberStatus;
import learntime.backend.domain.study.error.code.StudyErrorCode;
import learntime.backend.domain.study.error.exception.StudyException;
import learntime.backend.domain.study_member.dto.request.ChangeOwnerRequestDTO;
import learntime.backend.domain.study_member.model.StudyMember;
import learntime.backend.domain.study_member.repository.StudyMemberRepository;
import learntime.backend.global.utils.StudyAuthUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@RequiredArgsConstructor
@Service
public class StudyMemberService {

    private final StudyMemberRepository studyMemberRepository;
    private final UserBlockRepository userBlockRepository;

    @Transactional(readOnly = true)
    public List<StudyMemberResponseDTO> getAllStudyMember(Long studyId, Long userId) {
        boolean isStudyMember =
                studyMemberRepository.existsByStudy_StudyIdAndUser_UserIdAndStatus(
                        studyId,
                        userId,
                        StudyMemberStatus.ACTIVE
                );
        if (!isStudyMember) {
            throw new StudyException(StudyErrorCode.STUDY_MEMBER_NOT_FOUND);
        }

        List<StudyMember> studyMemberList = studyMemberRepository.findAllActiveByStudyIdFetchUser(studyId);

        Set<Long> blockedIds = userBlockRepository.findBlockedUserIds(userId);

        return studyMemberList.stream()
                .map(studyMember -> StudyMemberConverter.toStudyMemberResponseDTO(
                                studyMember,
                                blockedIds
                        )
                )
                .toList();
    }

    @Transactional
    public void changeStudyOwner(ChangeOwnerRequestDTO request, Long ownerId) {
        // 현재 OWNER 멤버 조회
        StudyMember ownerStudyMember = studyMemberRepository
                .findByStudy_StudyIdAndUser_UserIdAndStatus(request.studyId(), ownerId, StudyMemberStatus.ACTIVE)
                .orElseThrow(() -> new StudyException(StudyErrorCode.STUDY_MEMBER_NOT_FOUND));

        // 새 OWNER 멤버 조회
        StudyMember newOwnerStudyMember = studyMemberRepository
                .findByStudy_StudyIdAndUser_UserId(request.studyId(), request.newOwnerMemberId())
                .orElseThrow(() -> new StudyException(StudyErrorCode.STUDY_MEMBER_NOT_FOUND));

        if (!newOwnerStudyMember.isActive()) {
            throw new StudyException(StudyErrorCode.INACTIVE_STUDY_MEMBER);
        }

        // 방장과 바뀔 맴버가 동일 인물인지 확인
        if (ownerId.equals(newOwnerStudyMember.getUser().getUserId())) {
            throw new StudyException(StudyErrorCode.INVALID_OWNER_TRANSFER);
        }

        StudyAuthUtil.checkOwnerRole(ownerStudyMember); // 요청자가 방장인지 확인

        ownerStudyMember.changeRole(StudyMemberRole.MEMBER);
        newOwnerStudyMember.changeRole(StudyMemberRole.OWNER);
    }

    @Transactional
    public void kickStudyMember(Long studyId, Long memberIdToKick, Long requesterId) {
        // requester 검증 (방장인지)
        StudyMember requester = studyMemberRepository
                .findByStudy_StudyIdAndUser_UserIdAndStatus(studyId, requesterId, StudyMemberStatus.ACTIVE)
                .orElseThrow(() -> new StudyException(StudyErrorCode.STUDY_MEMBER_NOT_FOUND));

        StudyAuthUtil.checkOwnerRole(requester);

        // kick 대상 검증
        StudyMember memberToKick = studyMemberRepository
                .findByStudy_StudyIdAndUser_UserIdAndStatus(studyId, memberIdToKick, StudyMemberStatus.ACTIVE)
                .orElseThrow(() -> new StudyException(StudyErrorCode.STUDY_MEMBER_NOT_FOUND));

        if (memberToKick.getStudyMemberRole() == StudyMemberRole.OWNER) {
            throw new StudyException(StudyErrorCode.CANNOT_KICK_OWNER);
        }

        memberToKick.withdraw();
    }

    @Transactional
    public void leaveStudy(Long studyId, Long userId) {
        StudyMember studyMember = studyMemberRepository
                .findByStudy_StudyIdAndUser_UserIdAndStatus(studyId, userId, StudyMemberStatus.ACTIVE)
                .orElseThrow(() -> new StudyException(StudyErrorCode.STUDY_MEMBER_NOT_FOUND));

        if (studyMember.getStudyMemberRole() == StudyMemberRole.OWNER) {
            throw new StudyException(StudyErrorCode.OWNER_LEAVE_NOT_ALLOWED);
        }

        studyMember.withdraw();
    }

}
