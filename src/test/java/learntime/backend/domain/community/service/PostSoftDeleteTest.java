package learntime.backend.domain.community.service;

import learntime.backend.domain.community.model.Comment;
import learntime.backend.domain.community.model.Post;
import learntime.backend.domain.community.model.PostImage;
import learntime.backend.domain.community.repository.CommentRepository;
import learntime.backend.domain.community.repository.PostImageRepository;
import learntime.backend.domain.community.repository.PostRepository;
import learntime.backend.domain.user.model.User;
import learntime.backend.domain.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
class PostSoftDeleteTest {

    @Autowired
    private PostService postService;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private PostImageRepository postImageRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("게시글 삭제 시 하위 엔티티(이미지, 댓글)가 모두 soft delete 되어야 한다")
    void deletePost_SoftDeleteCascading() {
        // given
        User user = User.builder().email("test@test.com").name("tester").build();
        userRepository.save(user);

        Post post = Post.builder().title("제목").content("내용").user(user).build();
        postRepository.save(post);

        PostImage image = PostImage.builder().fileUrl("url").originalFileName("test.jpg").post(post).build();
        post.addImage(image); // 엔티티 관계 설정
        postImageRepository.save(image); // 별도 저장

        Comment comment = Comment.builder().content("댓글").post(post).user(user).build();
        commentRepository.save(comment); // 별도 저장

        entityManager.flush();
        entityManager.clear(); // 영속성 컨텍스트 초기화하여 DB 상태 확인 준비

        // when
        postService.deletePost(post.getPostId(), user.getUserId());
        entityManager.flush();
        entityManager.clear();

        // then
        // 1. 게시글이 DB에서 조회되지 않음 (Soft Delete 상태)
        assertThat(postRepository.findById(post.getPostId())).isEmpty();

        // 2. 이미지가 조회되지 않음 (Soft Delete Cascading 확인)
        assertThat(postImageRepository.findByPost_PostId(post.getPostId())).isEmpty();

        // 3. 댓글이 조회되지 않음 (Soft Delete Cascading 확인)
        assertThat(commentRepository.findFirstPageByPostIdWithUser(post.getPostId(), Pageable.unpaged())).isEmpty();
    }
}
