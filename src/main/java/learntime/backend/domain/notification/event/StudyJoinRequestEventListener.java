package learntime.backend.domain.notification.event;

import learntime.backend.domain.notification.enums.NotificationReferenceType;
import learntime.backend.domain.notification.enums.NotificationType;
import learntime.backend.domain.notification.service.NotificationService;
import learntime.backend.domain.study_member.event.StudyJoinRequestApprovedEvent;
import learntime.backend.domain.study_member.event.StudyJoinRequestCreatedEvent;
import learntime.backend.domain.study_member.event.StudyJoinRequestRejectedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class StudyJoinRequestEventListener {

    private final NotificationService notificationService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleStudyJoinRequestCreated(StudyJoinRequestCreatedEvent event) {
        try {
            String eventMessage = event.requesterName() + " 님이 [" + event.studyTitle() + "] 스터디 가입을 요청했습니다.";

            notificationService.notify(
                    event.ownerUserId(),
                    NotificationType.STUDY_JOIN_REQUEST_RECEIVED,
                    "스터디 가입 요청 도착",
                    eventMessage,
                    event.studyJoinRequestId(),
                    NotificationReferenceType.STUDY_JOIN_REQUEST
            );
        } catch (Exception e) {
            log.error("스터디 가입 요청 알림 생성 실패. studyJoinRequestId: {}", event.studyJoinRequestId(), e);
        }
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleStudyJoinRequestApproved(StudyJoinRequestApprovedEvent event) {
        try {
            String eventMessage = "[" + event.studyTitle() + "] 스터디 가입 요청이 승인되었습니다.";

            notificationService.notify(
                    event.requesterUserId(),
                    NotificationType.STUDY_JOIN_REQUEST_APPROVED,
                    "스터디 가입 요청 승인",
                    eventMessage,
                    event.studyJoinRequestId(),
                    NotificationReferenceType.STUDY_JOIN_REQUEST
            );
        } catch (Exception e) {
            log.error("스터디 가입 승인 알림 생성 실패. studyJoinRequestId: {}", event.studyJoinRequestId(), e);
        }
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleStudyJoinRequestRejected(StudyJoinRequestRejectedEvent event) {
        try {
            String eventMessage = "[" + event.studyTitle() + "] 스터디 가입 요청이 거절되었습니다.";

            notificationService.notify(
                    event.requesterUserId(),
                    NotificationType.STUDY_JOIN_REQUEST_REJECTED,
                    "스터디 가입 요청 거절",
                    eventMessage,
                    event.studyJoinRequestId(),
                    NotificationReferenceType.STUDY_JOIN_REQUEST
            );
        } catch (Exception e) {
            log.error("스터디 가입 거절 알림 생성 실패. studyJoinRequestId: {}", event.studyJoinRequestId(), e);
        }
    }
}
