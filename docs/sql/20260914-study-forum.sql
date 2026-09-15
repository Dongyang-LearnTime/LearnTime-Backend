-- MySQL 8: apply once before deploying this version, while application writes are stopped.
-- Existing installations: verify SHOW CREATE TABLE message before applying.
ALTER TABLE message MODIFY COLUMN sender_id BIGINT NULL,
                    MODIFY COLUMN receiver_id BIGINT NULL,
                    ADD COLUMN completely_deleted_at DATETIME(6) NULL;

-- Historical deletion times are unknown: start a fresh retention period.
UPDATE message SET completely_deleted_at = CURRENT_TIMESTAMP(6)
WHERE sender_deleted = TRUE AND receiver_deleted = TRUE AND completely_deleted_at IS NULL;

CREATE INDEX idx_message_cleanup ON message (completely_deleted_at);
CREATE INDEX idx_post_image_deleted_at ON post_image (deleted_at);
CREATE INDEX idx_user_deleted_at ON user (deleted_at);

ALTER TABLE study_member ADD CONSTRAINT uk_study_member_study_member UNIQUE (study_id, study_member_id);

CREATE TABLE study_forum_message (
    message_id BIGINT NOT NULL AUTO_INCREMENT,
    study_id BIGINT NOT NULL,
    study_member_id BIGINT NULL,
    content VARCHAR(1000) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (message_id),
    INDEX idx_forum_study_message (study_id, message_id),
    INDEX idx_forum_author (study_member_id),
    CONSTRAINT fk_forum_study FOREIGN KEY (study_id) REFERENCES study (study_id),
    CONSTRAINT fk_forum_member FOREIGN KEY (study_member_id) REFERENCES study_member (study_member_id),
    CONSTRAINT fk_forum_member_study FOREIGN KEY (study_id, study_member_id)
        REFERENCES study_member (study_id, study_member_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Detach historical withdrawn accounts too; Hibernate filters hide these users.
UPDATE message m JOIN user u ON u.user_id = m.sender_id
SET m.completely_deleted_at = CASE WHEN m.receiver_deleted = TRUE
        THEN COALESCE(m.completely_deleted_at, CURRENT_TIMESTAMP(6)) ELSE m.completely_deleted_at END,
    m.sender_deleted = TRUE, m.sender_id = NULL
WHERE u.deleted_at > '2000-01-01 00:00:00';
UPDATE message m JOIN user u ON u.user_id = m.receiver_id
SET m.completely_deleted_at = CASE WHEN m.sender_deleted = TRUE
        THEN COALESCE(m.completely_deleted_at, CURRENT_TIMESTAMP(6)) ELSE m.completely_deleted_at END,
    m.receiver_deleted = TRUE, m.receiver_id = NULL
WHERE u.deleted_at > '2000-01-01 00:00:00';
UPDATE study_join_request r JOIN user u ON u.user_id = r.requester_user_id
SET r.status = 'CANCELED', r.updated_at = CURRENT_TIMESTAMP(6)
WHERE r.status = 'PENDING' AND u.deleted_at > '2000-01-01 00:00:00';
