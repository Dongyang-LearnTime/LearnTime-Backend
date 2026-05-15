package learntime.backend.domain.notification.error.exception;

import learntime.backend.domain.notification.error.code.NotificationErrorCode;
import learntime.backend.global.error.exception.BaseException;

public class NotificationException extends BaseException {
    public NotificationException(NotificationErrorCode notificationErrorCode) {
        super(notificationErrorCode);
    }
}
