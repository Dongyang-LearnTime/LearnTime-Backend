package learntime.backend.global.error.exception;

import learntime.backend.global.error.code.ValidatorErrorCode;

public class ValidatorException extends BaseException {
  public ValidatorException(ValidatorErrorCode errorCode) {
    super(errorCode);
  }
}
