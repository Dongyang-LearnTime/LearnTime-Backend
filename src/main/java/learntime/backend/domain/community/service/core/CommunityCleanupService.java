package learntime.backend.domain.community.service.core;

import learntime.backend.domain.community.repository.CommentRepository;
import learntime.backend.domain.community.repository.PostRepository;
import learntime.backend.global.infra.s3.S3Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
    private final S3Service s3Service;

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

        // 5. S3에 저장된 실제 이미지 파일 삭제
        // S3 삭제 실패가 DB 삭제 롤백을 발생시키지 않도록 여기서 예외를 캐치합니다.
        for (String url : imageUrlsToDelete) {
            try {
                s3Service.deleteFile(url);
            } catch (Exception e) {
                log.error("Failed to delete S3 file during cleanup: {}", url, e);
            }
        }
        if (!imageUrlsToDelete.isEmpty()) {
            log.info("Requested deletion of {} images from S3.", imageUrlsToDelete.size());
        }
    }
}

