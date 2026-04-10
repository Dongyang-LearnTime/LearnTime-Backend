package learntime.backend.domain.study.error.exception;

import learntime.backend.domain.study.error.code.StudyErrorCode;
import learntime.backend.global.error.exception.BaseException;

public class StudyException extends BaseException {
    public StudyException(StudyErrorCode studyErrorCode) {
        super(studyErrorCode);
    }
}
