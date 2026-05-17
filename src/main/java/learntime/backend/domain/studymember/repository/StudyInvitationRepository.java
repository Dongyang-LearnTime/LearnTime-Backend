package learntime.backend.domain.studymember.repository;

import learntime.backend.domain.studymember.enums.StudyInvitationStatus;
import learntime.backend.domain.studymember.model.StudyInvitation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StudyInvitationRepository extends JpaRepository<StudyInvitation, Long> {

    // 특정 상태의 공부 진도 초대 요청이 있는지 확인
    boolean existsByStudy_StudyIdAndInvitedUser_UserIdAndStatus(
            Long studyId,
            Long invitedUserId,
            StudyInvitationStatus status
    );

}
