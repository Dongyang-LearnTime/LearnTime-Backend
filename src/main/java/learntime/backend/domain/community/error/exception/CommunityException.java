package learntime.backend.domain.community.error.exception;

import learntime.backend.domain.community.error.code.CommunityErrorCode;
import learntime.backend.domain.study.error.code.StudyErrorCode;
import learntime.backend.global.error.exception.BaseException;

public class CommunityException extends BaseException {
    public CommunityException(CommunityErrorCode communityErrorCode) {
        super(communityErrorCode);
    }
}
