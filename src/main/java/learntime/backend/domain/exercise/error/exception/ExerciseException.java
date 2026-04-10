package learntime.backend.domain.exercise.error.exception;

import learntime.backend.domain.exercise.error.code.ExerciseErrorCode;
import learntime.backend.global.error.exception.BaseException;

public class ExerciseException extends BaseException {
    public ExerciseException(ExerciseErrorCode exerciseErrorCode) {
        super(exerciseErrorCode);
    }
}
