package learntime.backend.domain.notification.event;

import learntime.backend.domain.notification.enums.NotificationType;
import learntime.backend.domain.notification.service.NotificationService;
import learntime.backend.domain.study.dto.event.StudyParticipantLeftEvent;
import learntime.backend.domain.study.dto.event.StudySharedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class StudyShareNotificationEventListener {

    private final NotificationService notificationService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleStudyShared(StudySharedEvent event) {
        try {
            // 공유 대상 친구에게 즉시 참가 알림을 저장하고 전송합니다.
            notificationService.notify(
                    event.memberId(),
                    NotificationType.STUDY_SHARED,
                    "공부 일정 공유",
                    event.ownerName() + "님이 '" + event.studyTitle() + "' 공부 일정에 함께 참여시켰습니다.",
                    event.studyId(),
                    "STUDY"
            );
        } catch (Exception e) {
            log.error("공유 공부 일정 알림 생성 실패. studyId: {}, memberId: {}", event.studyId(), event.memberId(), e);
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleStudyParticipantLeft(StudyParticipantLeftEvent event) {
        try {
            // 참가자가 나가면 생성자에게 알림을 저장하고 전송합니다.
            notificationService.notify(
                    event.ownerId(),
                    NotificationType.STUDY_PARTICIPANT_LEFT,
                    "공유 공부 일정 참가자 나감",
                    event.memberName() + "님이 '" + event.studyTitle() + "' 공부 일정에서 나갔습니다.",
                    event.studyId(),
                    "STUDY"
            );
        } catch (Exception e) {
            log.error("공유 공부 일정 나가기 알림 생성 실패. studyId: {}, memberId: {}", event.studyId(), event.memberId(), e);
        }
    }
}
