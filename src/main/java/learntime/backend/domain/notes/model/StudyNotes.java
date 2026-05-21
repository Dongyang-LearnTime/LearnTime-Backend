package learntime.backend.domain.notes.model;

import jakarta.persistence.*;
import learntime.backend.domain.study_member.model.StudyMember;
import learntime.backend.global.common.BaseTimeEntity;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "study_notes",
        indexes = {
                @Index(name = "idx_study_notes_member", columnList = "study_member_id")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StudyNotes extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long studyNotesId;

    // SET NULL로 연결
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "study_member_id")
    @OnDelete(action = OnDeleteAction.SET_NULL)
    private StudyMember studyMember;

    @Column(name = "notes_title", nullable = false)
    private String noteTitle;

    @Column(nullable = false, columnDefinition = "MEDIUMTEXT")
    private String noteContents;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Builder
    public StudyNotes(String noteTitle, String noteContents, StudyMember studyMember) {
        this.noteTitle = noteTitle;
        this.noteContents = noteContents;
        this.studyMember = studyMember;
    }

    public void update(String noteTitle, String noteContents) {
        this.noteTitle = noteTitle;
        this.noteContents = noteContents;
    }

}
