package uk.gov.dwp.uc.pairtest.exception;

import lombok.Getter;
import uk.gov.dwp.uc.pairtest.enums.ErrorCodes;

@Getter
public class SeatReservationException extends InvalidPurchaseException {
    private final ErrorCodes errorCode;
    public SeatReservationException(ErrorCodes errorCode, String errorMessage) {
        super(errorCode, errorMessage);
        this.errorCode = errorCode;
    }
}
