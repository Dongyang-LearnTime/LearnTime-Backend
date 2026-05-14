package learntime.backend.global.error.exception;

import learntime.backend.global.error.code.FileErrorCode;

public class FileException extends BaseException {
    public FileException(FileErrorCode fileErrorCode) {
        super(fileErrorCode);
    }
}
