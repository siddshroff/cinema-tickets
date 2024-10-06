package uk.gov.dwp.uc.pairtest.exception;

import lombok.Getter;
import uk.gov.dwp.uc.pairtest.enums.ErrorCodes;

@Getter
public class InvalidPurchaseException extends RuntimeException {
    private final ErrorCodes errorCode;
    public InvalidPurchaseException(ErrorCodes errorCode, String errorMessage) {
        super(errorMessage);
        this.errorCode = errorCode;
    }
}
