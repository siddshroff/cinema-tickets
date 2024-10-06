package uk.gov.dwp.uc.pairtest.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ErrorCodes {
    ERRORCT01("Unknown application error"),
    ERRORCT02("Account Id Invalid"),
    ERRORCT03("Max ticket count purchase exceeded"),
    ERRORCT04("Adult ticket is not present"),
    ERRORCT05("Purchase data is null");
    private final String msg;
}