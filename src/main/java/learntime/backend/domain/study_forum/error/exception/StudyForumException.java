package learntime.backend.domain.study_forum.error.exception;

import learntime.backend.domain.study_forum.error.code.StudyForumErrorCode;
import learntime.backend.global.error.exception.BaseException;

public class StudyForumException extends BaseException {
    public StudyForumException(StudyForumErrorCode code) {
        super(code);
    }
}

