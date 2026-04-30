package learntime.backend.domain.point.event;

import learntime.backend.domain.point.dto.PointEventDTO;
import learntime.backend.domain.point.model.PointHistory;
import learntime.backend.domain.point.repository.PointHistoryRepository;
import learntime.backend.domain.user.model.User;
import learntime.backend.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class PointEventListener {

    private final UserRepository userRepository;
    private final PointHistoryRepository pointHistoryRepository;

    // Service의 마지막 줄 (로직이 모두 끝난 후)에 넣어야 함 (@Transactional 명시 되있어야 함)
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handlePointEarnEvent(PointEventDTO event) {

        // 포인트 업데이트
        userRepository.updatePoint(event.userId(), event.amount());

        // User 엔티티 프록시 조회
        User userProxy = userRepository.getReferenceById(event.userId());

        // 포인트 내역 저장
        PointHistory history = PointHistory.builder()
                .user(userProxy)
                .amount(event.amount())
                .pointType(event.pointType())
                .description(event.description())
                .build();

        pointHistoryRepository.save(history);

        String action = switch (event.pointType()) {
            case EARN -> "지급";
            case USE -> "사용";
            case CANCEL -> "취소";
        };

        log.info("[Point Event] {} (email: {}) 에게 {}p {} 완료 (사유: {})",
                userProxy.getName(), userProxy.getEmail(), Math.abs(event.amount()), action, event.description());
    }

}
