package uk.gov.dwp.uc.pairtest.domain;

import lombok.Builder;
import lombok.Value;

/**
 * Immutable Object
 * This object can only be constructed. It has only getters and no setters
 */
public class TicketTypeRequest {

    private int noOfTickets;
    private Type type;

    public TicketTypeRequest(Type type, int noOfTickets) {
        this.type = type;
        this.noOfTickets = noOfTickets;
    }

    public int getNoOfTickets() {
        return noOfTickets;
    }

    public Type getTicketType() {
        return type;
    }

    public enum Type {
        ADULT, CHILD , INFANT
    }

}
