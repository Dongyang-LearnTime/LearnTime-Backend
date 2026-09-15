package learntime.backend.domain.study_forum.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import learntime.backend.domain.study.model.Study;
import learntime.backend.domain.study_member.model.StudyMember;
import jakarta.persistence.ForeignKey;
import learntime.backend.global.common.BaseTimeEntity;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "study_forum_message", indexes = {
        @Index(name = "idx_forum_study_message", columnList = "study_id, message_id"),
        @Index(name = "idx_forum_author", columnList = "study_member_id")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StudyForumMessage extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long messageId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "study_id", nullable = false)
    private Study study;

    @ManyToOne(fetch = FetchType.LAZY)
    // The migration adds a composite FK (study_id, study_member_id) to prevent cross-study authors.
    @JoinColumn(name = "study_member_id", foreignKey = @ForeignKey(name = "fk_forum_member"))
    private StudyMember author;

    @Column(nullable = false, length = 1000)
    private String content;

    @Builder
    public StudyForumMessage(Study study, StudyMember author, String content) {
        this.study = study;
        this.author = author;
        this.content = content;
    }
}

