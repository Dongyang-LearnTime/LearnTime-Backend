package learntime.backend.domain.study.repository;

import learntime.backend.domain.study.enums.StudyParticipantRole;
import learntime.backend.domain.study.model.StudyParticipant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
// 공부 일정 참가자의 참여 여부, 역할, 내 공유 일정 조회를 담당하는 Repository
public interface StudyParticipantRepository extends JpaRepository<StudyParticipant, Long> {

    // 특정 사용자가 현재 해당 공부 일정에 참여 중인지 조회
    Optional<StudyParticipant> findByStudy_StudyIdAndUser_UserIdAndLeftAtIsNull(Long studyId, Long userId);

    // 특정 공부 일정에 이미 등록된 사용자 여부 확인
    boolean existsByStudy_StudyIdAndUser_UserId(Long studyId, Long userId);

    // 특정 공부 일정의 현재 참가자 목록 조회
    List<StudyParticipant> findAllByStudy_StudyIdAndLeftAtIsNullOrderByCreatedAtAsc(Long studyId);

    // 특정 사용자가 현재 참여 중인 공부 일정 목록 조회
    List<StudyParticipant> findAllByUser_UserIdAndLeftAtIsNullOrderByCreatedAtDesc(Long userId);

    // 특정 공부 일정에 현재 참여 중인 친구 참가자 수 조회
    long countByStudy_StudyIdAndRoleAndLeftAtIsNull(Long studyId, StudyParticipantRole role);
}
