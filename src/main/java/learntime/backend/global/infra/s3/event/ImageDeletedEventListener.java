package learntime.backend.global.infra.s3.event;

import learntime.backend.global.infra.s3.S3Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class ImageDeletedEventListener {

    private final S3Service s3Service;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void handleImageDeletedEvent(ImageDeletedEvent event) {
        try {
            s3Service.deleteFile(event.imageUrl());
        } catch (Exception e) {
            log.error("Failed to delete S3 file asynchronously: {}", event.imageUrl(), e);
        }
    }
}
