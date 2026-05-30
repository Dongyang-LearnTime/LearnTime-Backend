package learntime.backend.domain.community.service;

import learntime.backend.domain.community.model.Comment;
import learntime.backend.domain.community.model.Post;
import learntime.backend.domain.community.model.PostImage;
import learntime.backend.domain.community.repository.CommentRepository;
import learntime.backend.domain.community.repository.PostImageRepository;
import learntime.backend.domain.community.repository.PostRepository;
import learntime.backend.domain.user.model.User;
import learntime.backend.domain.user.repository.UserRepository;
import learntime.backend.global.infra.s3.S3Service;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Field;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
class CommunityCleanupServiceTest {

    @Autowired
    private CommunityCleanupService communityCleanupService;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private PostImageRepository postImageRepository;

    @Autowired
    private UserRepository userRepository;

    @MockitoBean
    private S3Service s3Service;

    @Test
    @DisplayName("90일이 지난 삭제된 게시글과 댓글 및 연관 이미지 객체 hard delete 정상 동작 검증")
    void hardDeleteOldPostsAndComments() throws Exception {
        // given
        User user = User.builder()
                .email("test_" + java.util.UUID.randomUUID().toString().substring(0, 8) + "@test.com")
                .name("testUser_" + java.util.UUID.randomUUID().toString().substring(0, 8))
                .build();
        userRepository.save(user);

        // 삭제한 지 91일 된 게시글
        Post oldDeletedPost = Post.builder()
                .title("Old Deleted")
                .content("Content")
                .user(user)
                .build();
        setDeletedAt(oldDeletedPost, LocalDateTime.now().minusDays(91));
        postRepository.save(oldDeletedPost);

        PostImage oldDeletedPostImage = PostImage.builder()
                .fileUrl("https://s3.amazonaws.com/test-bucket/old.jpg")
                .originalFileName("old.jpg")
                .post(oldDeletedPost)
                .build();
        setDeletedAt(oldDeletedPostImage, LocalDateTime.now().minusDays(91));
        postImageRepository.save(oldDeletedPostImage);

        // 삭제한 지 10일 된 게시글
        Post recentDeletedPost = Post.builder()
                .title("Recent Deleted")
                .content("Content")
                .user(user)
                .build();
        setDeletedAt(recentDeletedPost, LocalDateTime.now().minusDays(10));
        postRepository.save(recentDeletedPost);

        // 삭제한 지 91일 된 독립 댓글
        Comment oldDeletedComment = Comment.builder()
                .content("Old Deleted Comment")
                .post(recentDeletedPost) // 아무 게시글이나 연결
                .user(user)
                .build();
        setDeletedAt(oldDeletedComment, LocalDateTime.now().minusDays(91));
        commentRepository.save(oldDeletedComment);

        // 삭제되지 않은 게시글
        Post activePost = Post.builder()
                .title("Active Post")
                .content("Content")
                .user(user)
                .build();
        postRepository.save(activePost);

        // when
        communityCleanupService.hardDeleteOldPostsAndComments();

        // then
        // 1. 91일 지난 게시글과 이미지는 DB에서 사라져야 함
        assertThat(postRepository.findById(oldDeletedPost.getPostId())).isEmpty();
        assertThat(postImageRepository.findById(oldDeletedPostImage.getPostImageId())).isEmpty();

        // 3. 91일 지난 독립 댓글은 삭제되어야 함
        assertThat(commentRepository.findById(oldDeletedComment.getCommentId())).isEmpty();

        // 4. S3Service의 deleteFile이 호출되었는지 검증 (URL 1개)
        verify(s3Service, times(1)).deleteFile(anyString());
    }

    private void setDeletedAt(Object entity, LocalDateTime dateTime) throws Exception {
        Class<?> clazz = entity.getClass();
        while (clazz != null && !clazz.getName().equals("java.lang.Object")) {
            try {
                Field field = clazz.getDeclaredField("deletedAt");
                field.setAccessible(true);
                field.set(entity, dateTime);
                return;
            } catch (NoSuchFieldException e) {
                clazz = clazz.getSuperclass();
            }
        }
    }
}
