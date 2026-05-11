package learntime.backend.domain.study.error.exception;

import learntime.backend.domain.study.error.code.FileErrorCode;
import learntime.backend.global.error.exception.BaseException;

public class FileException extends BaseException {
    public FileException(FileErrorCode fileErrorCode) {
        super(fileErrorCode);
    }
}
