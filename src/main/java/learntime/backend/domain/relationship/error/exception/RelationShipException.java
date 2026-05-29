package learntime.backend.domain.relationship.error.exception;

import learntime.backend.domain.relationship.error.code.RelationShipCode;
import learntime.backend.global.error.exception.BaseException;

public class RelationShipException extends BaseException {
    public RelationShipException(RelationShipCode relationShipCode) {
        super(relationShipCode);
    }
}
