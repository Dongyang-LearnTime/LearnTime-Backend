package learntime.backend.persistence;

import jakarta.persistence.EntityManager;
import learntime.backend.domain.community.model.Post;
import learntime.backend.domain.community.model.PostImage;
import learntime.backend.domain.community.service.CommunityCleanupService;
import learntime.backend.domain.message.converter.MessageConverter;
import learntime.backend.domain.message.model.Message;
import learntime.backend.domain.message.repository.MessageRepository;
import learntime.backend.domain.notes.model.StudyNotes;
import learntime.backend.domain.point.enums.PointType;
import learntime.backend.domain.point.model.PointHistory;
import learntime.backend.domain.quiz.enums.QuizType;
import learntime.backend.domain.quiz.model.QuizAnswer;
import learntime.backend.domain.quiz.model.QuizHistory;
import learntime.backend.domain.quiz.model.QuizQuestion;
import learntime.backend.domain.quiz.model.StudyQuiz;
import learntime.backend.domain.relationship.model.UserBlock;
import learntime.backend.domain.study.model.Study;
import learntime.backend.domain.study.model.StudyDailyPlan;
import learntime.backend.domain.study.service.core.StudyManagementService;
import learntime.backend.domain.study_feedback.model.StudyFeedback;
import learntime.backend.domain.study_forum.dto.request.StudyForumMessageRequestDTO;
import learntime.backend.domain.study_forum.error.exception.StudyForumException;
import learntime.backend.domain.study_forum.model.StudyForumMessage;
import learntime.backend.domain.study_forum.service.StudyForumService;
import learntime.backend.domain.study_member.enums.StudyMemberRole;
import learntime.backend.domain.study_member.enums.StudyMemberStatus;
import learntime.backend.domain.study_member.model.StudyInvitation;
import learntime.backend.domain.study_member.model.StudyJoinRequest;
import learntime.backend.domain.study_member.model.StudyMember;
import learntime.backend.domain.study_progress.model.StudyMemberContent;
import learntime.backend.domain.study_progress.model.StudyStatus;
import learntime.backend.domain.user.model.User;
import learntime.backend.domain.user.repository.UserRepository;
import learntime.backend.domain.user.service.OAuth2Service;
import learntime.backend.domain.user.service.UserService;
import learntime.backend.global.infra.s3.event.ImageDeletedEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.jdbc.core.JdbcTemplate;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(classes = ForumDeletionMySqlTest.Config.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE, properties = {
        "spring.config.location=optional:classpath:forum-test-empty.properties",
        "spring.datasource.url=${TEST_MYSQL_URL}",
        "spring.datasource.username=${TEST_MYSQL_USER:root}",
        "spring.datasource.password=${TEST_MYSQL_PASSWORD:}",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.open-in-view=false"
})
@EnabledIfEnvironmentVariable(named = "TEST_MYSQL_URL", matches = "jdbc:mysql:.*")
@Transactional
@RecordApplicationEvents
class ForumDeletionMySqlTest {
    @Configuration
    @EnableAutoConfiguration
    @EnableJpaAuditing
    @EntityScan("learntime.backend.domain")
    @EnableJpaRepositories("learntime.backend.domain")
    @Import({UserService.class, StudyForumService.class, StudyManagementService.class, CommunityCleanupService.class})
    static class Config {}

    @BeforeAll
    static void applyCompositeConstraint(@Autowired JdbcTemplate jdbc) {
        jdbc.execute("ALTER TABLE study_forum_message ADD CONSTRAINT fk_forum_member_study "
                + "FOREIGN KEY (study_id, study_member_id) REFERENCES study_member (study_id, study_member_id)");
    }

    @MockitoBean OAuth2Service oauth2Service;
    @Autowired EntityManager em;
    @Autowired UserRepository users;
    @Autowired UserService userService;
    @Autowired StudyForumService forum;
    @Autowired StudyManagementService studies;
    @Autowired MessageRepository messages;
    @Autowired CommunityCleanupService cleanup;
    @Autowired ApplicationEvents events;
    @Autowired PlatformTransactionManager transactions;

    @Test
    void hardDeleteRemovesNestedReferencesAndPreservesOtherMembersAndSharedMessages() {
        User old = user();
        User other = user();
        Study study = study();
        StudyMember oldMember = member(study, old, StudyMemberRole.MEMBER, StudyMemberStatus.ACTIVE);
        StudyMember otherMember = member(study, other, StudyMemberRole.OWNER, StudyMemberStatus.ACTIVE);
        learningRecords(oldMember);
        StudyNotes remainingNote = persist(StudyNotes.builder().studyMember(otherMember).noteTitle("keep").noteContents("keep").build());
        persist(StudyJoinRequest.builder().study(study).requesterUser(old).build());
        persist(StudyInvitation.builder().study(study).invitedUser(old).inviterUser(other).build());
        persist(PointHistory.builder().user(old).amount(10).pointType(PointType.EARN).description("earned").build());
        Message message = persist(new Message("keep inbox", old, other));
        StudyForumMessage shared = persist(StudyForumMessage.builder().study(study).author(oldMember).content("keep forum").build());
        // Exercise the cleanup against historical rows that still retain user references.
        flush();
        em.createNativeQuery("UPDATE user SET deleted_at = :date WHERE user_id = :id")
                .setParameter("date", LocalDateTime.now().minusDays(190)).setParameter("id", old.getUserId()).executeUpdate();
        em.clear();

        assertThat(users.hardDeleteOldUsers(LocalDateTime.now().minusDays(180))).isEqualTo(1);
        for (String table : new String[]{"quiz_answer", "quiz_history", "quiz_question", "study_quiz",
                "study_feedback", "study_status", "study_member_content", "study_invitation", "study_join_request", "point_history"}) {
            assertThat(count(table)).as(table).isZero();
        }
        assertThat(count("study")).isEqualTo(1);
        assertThat(count("study_member")).isEqualTo(1);
        assertThat(em.find(StudyNotes.class, remainingNote.getStudyNotesId())).isNotNull();
        assertThat(em.find(StudyForumMessage.class, shared.getMessageId()).getAuthor()).isNull();
        assertThat(MessageConverter.toMessageResponse(em.find(Message.class, message.getMessageId())).senderName())
                .isEqualTo("탈퇴한 사용자");
        assertThat(messages.findReceivedMessages(other.getUserId(), org.springframework.data.domain.PageRequest.of(0, 10)).getContent())
                .hasSize(1);
        assertThat(users.hardDeleteOldUsers(LocalDateTime.now().minusDays(180))).isZero();
        assertThat(users.findById(other.getUserId())).isPresent();
    }

    @Test
    void softDeleteCancelsPendingRequestsDetachesMessagesAndTransfersCompletedOwnership() {
        User owner = user();
        User other = user();
        Study study = study();
        member(study, owner, StudyMemberRole.OWNER, StudyMemberStatus.COMPLETED);
        StudyMember successor = member(study, other, StudyMemberRole.MEMBER, StudyMemberStatus.COMPLETED);
        StudyJoinRequest pending = persist(StudyJoinRequest.builder().study(study).requesterUser(owner).build());
        StudyJoinRequest accepted = StudyJoinRequest.builder().study(study).requesterUser(owner).build();
        accepted.approve();
        persist(accepted);
        Message message = persist(new Message("hello", owner, other));
        StudyForumMessage shared = persist(StudyForumMessage.builder().study(study).author(authorMembership(study, owner)).content("hello").build());
        flush();
        userService.deleteUser(owner.getUserId(), null);
        flush();
        assertThat(em.find(StudyJoinRequest.class, pending.getStudyJoinRequestId()).getStatus().name()).isEqualTo("CANCELED");
        assertThat(em.find(StudyJoinRequest.class, accepted.getStudyJoinRequestId()).getStatus().name()).isEqualTo("APPROVED");
        assertThat(em.find(StudyMember.class, successor.getStudyMemberId()).getStudyMemberRole()).isEqualTo(StudyMemberRole.OWNER);
        assertThat(em.find(Message.class, message.getMessageId()).getSender()).isNull();
        assertThat(em.find(StudyForumMessage.class, shared.getMessageId()).getAuthor()).isNull();
        assertThat(users.findById(owner.getUserId())).isEmpty();
    }

    @Test
    void unreadMessagesExpireFromCompleteDeletionTimeAndKeepOneSidedOrRecentMessages() {
        User a = user(), b = user();
        Message expired = persist(new Message("expired", a, b));
        expired.deleteBySender();
        expired.deleteByReceiver();
        Message recent = persist(new Message("recent", a, b));
        recent.deleteBySender();
        recent.deleteByReceiver();
        Message oneSided = persist(new Message("one-sided", a, b));
        oneSided.deleteBySender();
        flush();
        em.createNativeQuery("UPDATE message SET completely_deleted_at = :date WHERE message_id = :id")
                .setParameter("date", LocalDateTime.now().minusMonths(2)).setParameter("id", expired.getMessageId()).executeUpdate();
        assertThat(messages.deleteExpiredMessages(LocalDateTime.now().minusMonths(1))).isEqualTo(1);
        assertThat(messages.findById(expired.getMessageId())).isEmpty();
        assertThat(messages.findById(recent.getMessageId())).isPresent();
        assertThat(messages.findById(oneSided.getMessageId())).isPresent();
    }

    @Test
    void withdrawalOfBothParticipantsSetsCompleteDeletionTime() {
        User a = user(), b = user();
        Message message = persist(new Message("unread", a, b));
        flush();
        userService.deleteUser(a.getUserId(), null);
        userService.deleteUser(b.getUserId(), null);
        Message saved = em.find(Message.class, message.getMessageId());
        assertThat(saved.getCompletelyDeletedAt()).isNotNull();
        assertThat(saved.getReadAt()).isNull();
        assertThat(saved.getSender()).isNull();
        assertThat(saved.getReceiver()).isNull();
    }

    @Test
    void individuallyDeletedImagesArePurgedWhileLivePostAndRecentImagesRemain() {
        Post post = persist(Post.builder().user(user()).title("live").content("live").build());
        PostImage expired = persist(PostImage.builder().post(post).fileUrl("https://example.test/old.png").originalFileName("old.png").build());
        PostImage recent = persist(PostImage.builder().post(post).fileUrl("https://example.test/recent.png").originalFileName("recent.png").build());
        flush();
        em.createNativeQuery("UPDATE post_image SET deleted_at = :date WHERE post_image_id = :id")
                .setParameter("date", LocalDateTime.now().minusDays(100)).setParameter("id", expired.getPostImageId()).executeUpdate();
        em.createNativeQuery("UPDATE post_image SET deleted_at = CURRENT_TIMESTAMP WHERE post_image_id = :id")
                .setParameter("id", recent.getPostImageId()).executeUpdate();
        cleanup.hardDeleteOldPostsAndComments();
        assertThat(count("post_image")).isEqualTo(1);
        assertThat(count("post")).isEqualTo(1);
        assertThat(events.stream(ImageDeletedEvent.class).count()).isEqualTo(1);
    }

    @Test
    void completedOwnerCanDeleteStudyIncludingForumAndNestedRecords() {
        User owner = user();
        Study study = study();
        StudyMember membership = member(study, owner, StudyMemberRole.OWNER, StudyMemberStatus.COMPLETED);
        learningRecords(membership);
        persist(StudyForumMessage.builder().study(study).author(authorMembership(study, owner)).content("message").build());
        flush();
        studies.deleteStudyBulk(study.getStudyId(), owner.getUserId());
        flush();
        assertThat(count("study")).isZero();
        assertThat(count("study_forum_message")).isZero();
        assertThat(count("quiz_answer")).isZero();
        assertThat(users.findById(owner.getUserId())).isPresent();
    }

    @Test
    void forumPaginationIsolationAndBothDirectionsOfBlocking() {
        User author = user(), viewer = user(), blockedBy = user();
        Study study = study(), anotherStudy = study();
        member(study, viewer, StudyMemberRole.MEMBER, StudyMemberStatus.ACTIVE);
        StudyForumMessage first = persist(StudyForumMessage.builder().study(study).author(authorMembership(study, viewer)).content("first").build());
        persist(StudyForumMessage.builder().study(study).author(authorMembership(study, author)).content("hidden").build());
        persist(StudyForumMessage.builder().study(study).author(authorMembership(study, blockedBy)).content("also hidden").build());
        persist(StudyForumMessage.builder().study(study).content("anonymous").build());
        persist(StudyForumMessage.builder().study(anotherStudy).author(authorMembership(anotherStudy, viewer)).content("other study").build());
        persist(UserBlock.builder().blocker(viewer).blocked(author).build());
        persist(UserBlock.builder().blocker(blockedBy).blocked(viewer).build());
        flush();
        var page = forum.getMessages(study.getStudyId(), viewer.getUserId(), null, 1);
        assertThat(page.hasNext()).isTrue();
        assertThat(page.content().getFirst().authorName()).isEqualTo("탈퇴한 사용자");
        var lastPage = forum.getMessages(study.getStudyId(), viewer.getUserId(), page.nextCursor(), 1);
        assertThat(lastPage.content().getFirst().messageId()).isEqualTo(first.getMessageId());
        assertThat(lastPage.hasNext()).isFalse();
    }

    @Test
    void forumCreateDeleteAndCompletedReadOnly() {
        User author = user();
        Study study = study();
        StudyMember membership = member(study, author, StudyMemberRole.MEMBER, StudyMemberStatus.ACTIVE);
        flush();
        var created = forum.create(study.getStudyId(), author.getUserId(), new StudyForumMessageRequestDTO("  hello  "));
        assertThat(created.content()).isEqualTo("hello");
        assertThat(created.createdAt()).isNotNull();
        em.find(StudyMember.class, membership.getStudyMemberId()).complete();
        flush();
        assertThat(forum.getMessages(study.getStudyId(), author.getUserId(), null, 30).content()).hasSize(1);
        assertThatThrownBy(() -> forum.create(study.getStudyId(), author.getUserId(), new StudyForumMessageRequestDTO("new")))
                .isInstanceOf(StudyForumException.class);
    }

    @Test
    void completedAuthorCanDeleteOwnMessage() {
        User author = user();
        Study study = study();
        member(study, author, StudyMemberRole.MEMBER, StudyMemberStatus.COMPLETED);
        StudyForumMessage message = persist(StudyForumMessage.builder().study(study).author(authorMembership(study, author)).content("delete").build());
        flush();
        forum.delete(study.getStudyId(), message.getMessageId(), author.getUserId());
        flush();
        assertThat(count("study_forum_message")).isZero();
    }

    @Test
    void withdrawnAndNonmemberCannotReadForum() {
        User withdrawn = user(), outsider = user();
        Study study = study();
        member(study, withdrawn, StudyMemberRole.MEMBER, StudyMemberStatus.WITHDRAWN);
        flush();
        assertThatThrownBy(() -> forum.getMessages(study.getStudyId(), withdrawn.getUserId(), null, 30)).isInstanceOf(StudyForumException.class);
        assertThatThrownBy(() -> forum.getMessages(study.getStudyId(), outsider.getUserId(), null, 30)).isInstanceOf(StudyForumException.class);
    }

    @Test
    void memberCannotDeleteAnotherAuthorsMessage() {
        User author = user(), other = user();
        Study study = study();
        member(study, other, StudyMemberRole.OWNER, StudyMemberStatus.ACTIVE);
        StudyForumMessage message = persist(StudyForumMessage.builder().study(study).author(authorMembership(study, author)).content("keep").build());
        flush();
        assertThatThrownBy(() -> forum.delete(study.getStudyId(), message.getMessageId(), other.getUserId()))
                .isInstanceOf(StudyForumException.class);
    }

    @Test
    void databaseRejectsAuthorFromDifferentStudy() {
        User author = user();
        Study first = study(), second = study();
        StudyMember wrongMember = member(second, author, StudyMemberRole.MEMBER, StudyMemberStatus.ACTIVE);
        flush();
        assertThatThrownBy(() -> persist(StudyForumMessage.builder().study(first).author(wrongMember).content("invalid").build()))
                .isInstanceOf(org.hibernate.exception.ConstraintViolationException.class);
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void concurrentCreationAndStudyDeletionLeaveNoOrphanMessages() throws Exception {
        TransactionTemplate tx = new TransactionTemplate(transactions);
        Long[] ids = tx.execute(status -> {
            User owner = user();
            Study study = study();
            member(study, owner, StudyMemberRole.OWNER, StudyMemberStatus.ACTIVE);
            return new Long[]{owner.getUserId(), study.getStudyId()};
        });
        CountDownLatch created = new CountDownLatch(1);
        CountDownLatch deleting = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var writer = executor.submit(() -> tx.execute(status -> {
                forum.create(ids[1], ids[0], new StudyForumMessageRequestDTO("concurrent"));
                created.countDown();
                await(deleting);
                return null;
            }));
            var deleter = executor.submit(() -> {
                await(created);
                deleting.countDown();
                studies.deleteStudyBulk(ids[1], ids[0]);
            });
            writer.get(15, TimeUnit.SECONDS);
            deleter.get(15, TimeUnit.SECONDS);
            tx.executeWithoutResult(status -> {
                assertThat(em.find(Study.class, ids[1])).isNull();
                assertThat(count("study_forum_message")).isZero();
            });
        } finally {
            tx.executeWithoutResult(status -> {
                if (em.find(Study.class, ids[1]) != null) {
                    studies.deleteStudyBulk(ids[1], ids[0]);
                }
                em.createNativeQuery("DELETE FROM user WHERE user_id = :id").setParameter("id", ids[0]).executeUpdate();
            });
        }
    }

    private static void await(CountDownLatch latch) {
        try {
            if (!latch.await(5, TimeUnit.SECONDS)) {
                throw new AssertionError("Concurrent operation did not start in time");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AssertionError(e);
        }
    }

    private StudyMember authorMembership(Study study, User user) {
        var rows = em.createQuery("SELECT m FROM StudyMember m WHERE m.study = :study AND m.user = :user", StudyMember.class)
                .setParameter("study", study).setParameter("user", user).getResultList();
        return rows.isEmpty() ? member(study, user, StudyMemberRole.MEMBER, StudyMemberStatus.ACTIVE) : rows.getFirst();
    }

    private User user() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        return persist(User.builder().email(suffix + "@test.local").name(suffix).password("test").build());
    }

    private Study study() {
        return persist(Study.builder().studyTitle("test").bookTitle("book").startDate(LocalDate.now())
                .endDate(LocalDate.now().plusDays(1)).build());
    }

    private StudyMember member(Study study, User user, StudyMemberRole role, StudyMemberStatus status) {
        return persist(StudyMember.builder().study(study).user(user).studyMemberRole(role).status(status).build());
    }

    private void learningRecords(StudyMember member) {
        StudyDailyPlan plan = persist(StudyDailyPlan.builder().study(member.getStudy()).dayNumber(1).planDate(LocalDate.now()).planContent("plan").build());
        persist(StudyNotes.builder().studyMember(member).noteTitle("note").noteContents("body").build());
        persist(StudyFeedback.builder().studyMember(member).feedbackTitle("feedback").feedbackContent("body").build());
        persist(StudyStatus.builder().studyMember(member).studyDailyPlan(plan).build());
        persist(StudyMemberContent.builder().studyMember(member).studyDailyPlan(plan).memberContent("content").build());
        StudyQuiz quiz = persist(StudyQuiz.builder().studyMember(member).quizTitle("quiz").build());
        QuizQuestion question = persist(QuizQuestion.builder().studyQuiz(quiz).questionContent("question").correctAnswer("answer")
                .quizType(QuizType.values()[0]).build());
        QuizHistory history = persist(QuizHistory.builder().studyQuiz(quiz).attemptNumber(1).correctCount(1).build());
        persist(QuizAnswer.builder().quizHistory(history).quizQuestion(question).userAnswer("answer").isCorrect(true).build());
    }

    private <T> T persist(T entity) { em.persist(entity); return entity; }
    private void flush() { em.flush(); em.clear(); }
    private long count(String table) {
        return ((Number) em.createNativeQuery("SELECT COUNT(*) FROM " + table).getSingleResult()).longValue();
    }
}

