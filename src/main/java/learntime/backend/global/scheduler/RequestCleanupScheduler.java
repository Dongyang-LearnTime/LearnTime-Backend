package learntime.backend.global.scheduler;

import learntime.backend.domain.studymember.enums.StudyInvitationStatus;
import learntime.backend.domain.studymember.repository.StudyInvitationRepository;
import learntime.backend.domain.user.enums.FriendRequestStatus;
import learntime.backend.domain.user.repository.FriendRequestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class RequestCleanupScheduler {

    private final FriendRequestRepository friendRequestRepository;
    private final StudyInvitationRepository studyInvitationRepository;

    // 매일 새벽 2시 10분에 실행 (거절된 친구 요청 삭제)
    @Scheduled(cron = "0 10 2 * * *", zone = "Asia/Seoul")
    @Transactional
    public void cleanupOldRejectedFriendRequests() {
        log.info("[친구 요청 정리] 스케줄러(새벽 2시 10분) 거절된 친구 요청 삭제 작업 실행");
        try {
            LocalDateTime cutoffDate = LocalDateTime.now().minusDays(30);
            friendRequestRepository.deleteOldRequestsByStatus(FriendRequestStatus.REJECTED, cutoffDate);
            log.info("[친구 요청 정리 완료] 거절된 지 30일이 지난 친구 요청 삭제 완료");
        } catch (Exception e) {
            log.error("[친구 요청 정리 실패] 삭제 작업 중 오류 발생", e);
        }
    }

    // 매일 새벽 2시 20분에 실행 (거절되거나 취소된 스터디 초대 요청 삭제)
    @Scheduled(cron = "0 20 2 * * *", zone = "Asia/Seoul")
    @Transactional
    public void cleanupOldRejectedStudyInvitations() {
        log.info("[스터디 초대 정리] 스케줄러(새벽 2시 20분) 거절/취소된 스터디 초대 삭제 작업 실행");
        try {
            LocalDateTime cutoffDate = LocalDateTime.now().minusDays(30);
            
            // 거절된 초대 삭제
            studyInvitationRepository.deleteOldInvitationsByStatus(StudyInvitationStatus.REJECTED, cutoffDate);
            // 취소된 초대 삭제 (필요한 경우)
            studyInvitationRepository.deleteOldInvitationsByStatus(StudyInvitationStatus.CANCELED, cutoffDate);
            
            log.info("[스터디 초대 정리 완료] 거절/취소된 지 30일이 지난 스터디 초대 삭제 완료");
        } catch (Exception e) {
            log.error("[스터디 초대 정리 실패] 삭제 작업 중 오류 발생", e);
        }
    }
}
