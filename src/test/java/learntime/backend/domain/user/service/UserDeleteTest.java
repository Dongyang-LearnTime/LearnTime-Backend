package learntime.backend.domain.user.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import learntime.backend.domain.community.model.Comment;
import learntime.backend.domain.community.model.Post;
import learntime.backend.domain.community.repository.CommentRepository;
import learntime.backend.domain.community.repository.PostRepository;
import learntime.backend.domain.study.model.Study;
import learntime.backend.domain.study_member.enums.StudyMemberRole;
import learntime.backend.domain.study_member.enums.StudyMemberStatus;
import learntime.backend.domain.study_member.model.StudyMember;
import learntime.backend.domain.user.model.User;
import learntime.backend.domain.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class UserDeleteTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private EntityManager em;

    @Test
    @DisplayName("사용자 탈퇴 시 모든 연관관계가 정리되고 게시글/댓글 작성자는 NULL이 되며 Study OWNER가 승계된다")
    void deleteUser_SoftDeleteAndAssociationCleanup() {
        // given: 1. 탈퇴할 사용자
        User withdrawingUser = User.builder()
                .email("withdraw@test.com")
                .name("탈퇴예정자")
                .password("password")
                .build();
        userRepository.save(withdrawingUser);

        // given: 2. 남을 사용자 (Study OWNER 승계 검증용)
        User remainingUser = User.builder()
                .email("remain@test.com")
                .name("잔류자")
                .password("password")
                .build();
        userRepository.save(remainingUser);

        // given: 3. Post 및 Comment 생성
        Post post = Post.builder()
                .title("탈퇴 예정자의 글")
                .content("내용")
                .user(withdrawingUser)
                .build();
        postRepository.save(post);

        Comment comment = Comment.builder() // (참고: Comment 생성자는 프로젝트에 맞게 수정 필요)
                .post(post)
                .user(withdrawingUser)
                .content("탈퇴 예정자의 댓글")
                .build();
        commentRepository.save(comment);

        // given: 4. Study 생성 및 멤버 설정 (OWNER - 탈퇴 예정자, ACTIVE MEMBER - 잔류자)
        Study study = Study.builder()
                .studyTitle("테스트 스터디")
                .bookTitle("테스트 책")
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusDays(10))
                .isPublic(false)
                .build();
        em.persist(study);
        em.flush();

        StudyMember ownerMember = StudyMember.builder()
                .study(study)
                .user(withdrawingUser)
                .studyMemberRole(StudyMemberRole.OWNER)
                .status(StudyMemberStatus.ACTIVE)
                .joinedAt(LocalDateTime.now())
                .build();
        em.persist(ownerMember);

        StudyMember activeMember = StudyMember.builder()
                .study(study)
                .user(remainingUser)
                .studyMemberRole(StudyMemberRole.MEMBER)
                .status(StudyMemberStatus.ACTIVE)
                .joinedAt(LocalDateTime.now())
                .build();
        em.persist(activeMember);

        // 연관관계 영속성 컨텍스트 플러시
        em.flush();
        em.clear();

        // when: 탈퇴 수행
        userService.deleteUser(withdrawingUser.getUserId(), null);

        // DB 반영
        em.flush();
        em.clear();

        // then: [검증 1] 게시글/댓글 보존 및 작성자 NULL 처리 검증
        Post savedPost = postRepository.findById(post.getPostId()).orElseThrow();
        assertThat(savedPost.getUser()).isNull();

        Comment savedComment = commentRepository.findById(comment.getCommentId()).orElseThrow();
        assertThat(savedComment.getUser()).isNull();

        // then: [검증 2] Study OWNER 승계 테스트
        Query studyMemberQuery = em.createQuery(
                "SELECT sm FROM StudyMember sm WHERE sm.study.studyId = :studyId AND sm.studyMemberRole = :role", StudyMember.class);
        studyMemberQuery.setParameter("studyId", study.getStudyId());
        studyMemberQuery.setParameter("role", StudyMemberRole.OWNER);

        List<StudyMember> newOwners = studyMemberQuery.getResultList();
        assertThat(newOwners).hasSize(1);
        assertThat(newOwners.get(0).getUser().getUserId()).isEqualTo(remainingUser.getUserId());

        // then: [검증 3] 사용자 비식별화 검증 (native query)
        Query userQuery = em.createNativeQuery(
                "SELECT email, name, password, social_id, deleted_at FROM user WHERE user_id = :userId");
        userQuery.setParameter("userId", withdrawingUser.getUserId());
        Object[] deletedUser = (Object[]) userQuery.getSingleResult();

        String deletedEmail = (String) deletedUser[0];
        String deletedName = (String) deletedUser[1];
        String deletedPassword = (String) deletedUser[2];
        String deletedSocialId = (String) deletedUser[3];
        LocalDateTime deletedAt = null;
        if (deletedUser[4] instanceof java.sql.Timestamp) {
            deletedAt = ((java.sql.Timestamp) deletedUser[4]).toLocalDateTime();
        } else if (deletedUser[4] instanceof java.time.LocalDateTime) {
            deletedAt = (java.time.LocalDateTime) deletedUser[4];
        }

        assertThat(deletedEmail).startsWith("deleted_");
        assertThat(deletedName).startsWith("탈퇴한 사용자_");
        assertThat(deletedPassword).isNull();
        assertThat(deletedSocialId).isNull();
        assertThat(deletedAt).isNotNull().isNotEqualTo(LocalDateTime.of(1970, 1, 1, 0, 0));
    }

    @Test
    @DisplayName("Hard Delete 시 deletedAt 기준 180일이 지난 사용자는 영구 삭제된다")
    void testHardDeleteOldUsers() {
        // given
        User oldUser = User.builder()
                .email("old@test.com")
                .name("오래된사용자")
                .password("1234")
                .build();
        userRepository.save(oldUser);

        em.flush();
        em.clear();

        // 1. Soft delete 처리 수행
        userService.deleteUser(oldUser.getUserId(), null);

        // 2. 190일 전으로 deletedAt 강제 업데이트 (Native Query)
        LocalDateTime pastDate = LocalDateTime.now().minusDays(190);
        em.createNativeQuery("UPDATE user SET deleted_at = :pastDate WHERE user_id = :userId")
                .setParameter("pastDate", pastDate)
                .setParameter("userId", oldUser.getUserId())
                .executeUpdate();

        em.flush();
        em.clear();

        // when: 180일이 지난 사용자 삭제 배치 실행
        LocalDateTime thresholdDate = LocalDateTime.now().minusDays(180);
        int deletedCount = userRepository.hardDeleteOldUsers(thresholdDate);

        // then: 삭제 확인 (1건 이상 삭제되어야 함)
        assertThat(deletedCount).isGreaterThanOrEqualTo(1);

        // DB에서 물리적(완전) 삭제 확인
        Query countQuery = em.createNativeQuery("SELECT count(*) FROM user WHERE user_id = :userId")
                .setParameter("userId", oldUser.getUserId());
        assertThat(((Number) countQuery.getSingleResult()).longValue()).isZero();
    }
}