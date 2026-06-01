package learntime.backend.domain.community.service;

import learntime.backend.domain.community.repository.CommentRepository;
import learntime.backend.domain.community.repository.PostRepository;
import learntime.backend.global.infra.s3.S3Service;
import learntime.backend.global.infra.s3.event.ImageDeletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommunityCleanupService {

    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public void hardDeleteOldPostsAndComments() {
        LocalDateTime threshold = LocalDateTime.now().minusDays(90);
        log.info("Starting hard delete for posts and comments deleted before: {}", threshold);

        // 1. 삭제될 게시글의 이미지 URL 미리 조회 (S3 객체 삭제용)
        List<String> imageUrlsToDelete = postRepository.findDeletedPostImageUrlsBefore(threshold);

        // 2. 게시글과 연관된 하위 엔티티들 하드 딜리트 (외래키 제약조건 고려)
        postRepository.hardDeletePostViewHistoryByDeletedPostThreshold(threshold);
        postRepository.hardDeletePostLikeByDeletedPostThreshold(threshold);
        postRepository.hardDeletePostImageByDeletedPostThreshold(threshold);

        // 연관된 댓글 삭제 (이 게시글에 속한 댓글)
        postRepository.hardDeleteCommentByDeletedPostThreshold(threshold);

        // 3. 90일 지난 게시글 삭제
        int deletedPostCount = postRepository.hardDeletePostByThreshold(threshold);
        log.info("Hard deleted {} posts.", deletedPostCount);

        // 4. (게시글과 무관하게) 개별적으로 삭제된 지 90일 지난 댓글 삭제
        int deletedCommentCount = commentRepository.hardDeleteCommentByThreshold(threshold);
        log.info("Hard deleted {} standalone comments.", deletedCommentCount);

        // 5. S3에 저장된 실제 이미지 파일 비동기 삭제를 위한 이벤트 발행
        for (String url : imageUrlsToDelete) {
            eventPublisher.publishEvent(new ImageDeletedEvent(url));
        }
        if (!imageUrlsToDelete.isEmpty()) {
            log.info("Published ImageDeletedEvent for {} images from S3.", imageUrlsToDelete.size());
        }
    }
}

