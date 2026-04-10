package learntime.backend.global.error.exception;

import learntime.backend.global.error.code.BaseErrorCode;
import lombok.Getter;

@Getter
public abstract class BaseException extends RuntimeException {
    private final BaseErrorCode errorCode;

    protected BaseException(BaseErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    // 상세 메시지가 필요한 경우 사용
    protected BaseException(BaseErrorCode errorCode, String detailMessage) {
        super(detailMessage);
        this.errorCode = errorCode;
    }

    @Override
    public synchronized Throwable fillInStackTrace() {
        // 스택 트레이스 생성을 생략하여 성능 최적화 (비즈니스 예외 한정)
        return this;
    }

}