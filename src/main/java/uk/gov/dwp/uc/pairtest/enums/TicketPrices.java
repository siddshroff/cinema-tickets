package uk.gov.dwp.uc.pairtest.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum TicketPrices {
    ADULT(20),
    CHILD(10),
    INFANT(0);
    private final Integer price;
}