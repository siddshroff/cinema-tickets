package uk.gov.dwp.uc.pairtest;

import io.prometheus.client.Counter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import thirdparty.paymentgateway.TicketPaymentService;
import thirdparty.seatbooking.SeatReservationService;
import uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest;
import uk.gov.dwp.uc.pairtest.enums.ErrorCodes;
import uk.gov.dwp.uc.pairtest.enums.TicketPrices;
import uk.gov.dwp.uc.pairtest.exception.InvalidPurchaseException;
import uk.gov.dwp.uc.pairtest.exception.PaymentGatewayException;
import uk.gov.dwp.uc.pairtest.exception.SeatReservationException;
import uk.gov.dwp.uc.pairtest.utils.Constants;

import java.util.Arrays;

/**
 * This service class for cinema ticket booking application.
 * This exposes the prometheus metrics for the failure events in the methods
 * It also have the service interfaces to Payment and Seat reservation services
 * for methods to consume.
 *
 * @author Siddharth Shroff
 * @version 1.0
 * @since 06-10-2024
 */
@RequiredArgsConstructor
public class TicketServiceImpl implements TicketService {

    @NonNull
    private final SeatReservationService seatReservationService;
    @NonNull
    private final TicketPaymentService ticketPaymentService;
    Logger logger = LoggerFactory.getLogger(TicketServiceImpl.class);

    // Prometheus metrics counter for hard failed events
    Counter failedEventsCounter =
            Counter.build().name("cinema_ticket_failure_events_total").help("total failure number of events").register();
    // Prometheus metrics counter for failed business validations resulting into failed transaction
    Counter failedBusinessValidationCounter =
            Counter.build().name("cinema_ticket_business_failure_events_total").help("total failed business validations").register();

    /**
     * This is the implementation method which purchase ticket for an account.
     * It takes in two arguments. i.e account ID and object/s of TicketTypeRequest.
     * It makes payment and then reserves the seat according to the request.
     * It checks for basic business validations as mentioned in Readme.md.
     * If any validation fails then throw exception.
     *
     * @param accountId
     * @param ticketTypeRequests
     * @throws InvalidPurchaseException
     */

    @Override
    public void purchaseTickets(Long accountId, TicketTypeRequest... ticketTypeRequests)
            throws InvalidPurchaseException {
        int totalAmountToPay = 0, totalSeatsToAllocate = 0;
        logger.debug("Validating requests for Account ID::", accountId);
        validateRequest(accountId, ticketTypeRequests);

        for (TicketTypeRequest ticketRequest : ticketTypeRequests) {
            totalAmountToPay += TicketPrices.valueOf(ticketRequest.getTicketType().name()).getPrice()
                    * ticketRequest.getNoOfTickets();

            totalSeatsToAllocate += !(ticketRequest.getTicketType().equals(TicketTypeRequest.Type.INFANT)) ?
                    ticketRequest.getNoOfTickets() : 0;
        }
        try {
            logger.debug("Proceeding for payment for Account ID:: {}", accountId);
            ticketPaymentService.makePayment(accountId, totalAmountToPay);
            logger.debug("Payment successful for Account ID:: {}", accountId);
        } catch (PaymentGatewayException e) {
            logger.error("Payment gateway failed to process payment", e);
            failedEventsCounter.inc();
            throw new PaymentGatewayException(
                    ErrorCodes.ERRORCT01, "Payment failed for Account id::" + accountId);
        }
        try {
            logger.debug("Proceeding for seat reservation for Account ID:: {}", accountId);
            seatReservationService.reserveSeat(accountId, totalSeatsToAllocate);
            logger.debug("Seat reservation successful for Account ID:: {}", accountId);
        } catch (SeatReservationException e) {
            logger.error("Seat reservation failed to reserve seat", e);
            failedEventsCounter.inc();
            throw new SeatReservationException(
                    ErrorCodes.ERRORCT01, "Seat reservation failed for Account id::" + accountId);
        }
    }

    /**
     * This method validates
     * 1. Valid check account id
     * 2. If maximum number of tickets are exceeded.
     * 3. If atleast one adult is booking the tickets
     * 4. Infant tickets should be less than or equal to adult ticket. One infant per adult
     *
     * @param accountId
     * @param ticketTypeRequests
     */
    private void validateRequest(Long accountId, TicketTypeRequest... ticketTypeRequests) {
        if (accountId == null || accountId <= 0) {
            logger.error("Invalid Account ID:: {}", accountId);
            failedBusinessValidationCounter.inc();
            throw new InvalidPurchaseException(
                    ErrorCodes.ERRORCT02, "Account ID is not a valid data");
        }
        if (isMaxTicketCountExceeded(ticketTypeRequests)) {
            logger.error("Request for maximum number of tickets exceeded", ticketTypeRequests);
            failedBusinessValidationCounter.inc();
            throw new InvalidPurchaseException(
                    ErrorCodes.ERRORCT03, "Max ticket purchase count exceed the limit of " + Constants.MAX_TICKETS_ALLOWED);
        }
        if (!isAdultTicketPresent(ticketTypeRequests)) {
            logger.error("Request having no adults", ticketTypeRequests);
            failedBusinessValidationCounter.inc();
            throw new InvalidPurchaseException(
                    ErrorCodes.ERRORCT04,
                    "No adult ticket is present for Account ID::" + accountId);
        }
        if (!isInfantTicketEqualAdultTicket(ticketTypeRequests)) {
            logger.error("Request having more infants than adults", ticketTypeRequests);
            failedBusinessValidationCounter.inc();
            throw new InvalidPurchaseException(
                    ErrorCodes.ERRORCT04,
                    "Adult tickets less than infant tickets for Account ID" + accountId);
        }
    }

    /**
     * This method validates
     * 1. If there are atleast equal number of adult to infants.
     *
     * @param ticketTypeRequests
     * @return boolean value of validation
     */
    private boolean isInfantTicketEqualAdultTicket(TicketTypeRequest[] ticketTypeRequests) {
        return Arrays.stream(ticketTypeRequests).filter(e -> e.getTicketType().equals(TicketTypeRequest.Type.ADULT))
                .map(i -> i.getNoOfTickets()).mapToInt(Integer::intValue).sum()
                < Arrays.stream(ticketTypeRequests).filter(e -> e.getTicketType().equals(TicketTypeRequest.Type.INFANT))
                .map(i -> i.getNoOfTickets()).mapToInt(Integer::intValue).sum() ? false : true;
    }

    /**
     * This method validates
     * 1. If maximum number of tickets are exceeded.
     *
     * @param ticketTypeRequests
     * @return boolean value of validation
     */
    private boolean isMaxTicketCountExceeded(TicketTypeRequest... ticketTypeRequests) {
        int totalNoOfTickets = Arrays.stream(ticketTypeRequests)
                .filter(e -> !e.getTicketType().equals(TicketTypeRequest.Type.INFANT))
                .mapToInt(TicketTypeRequest::getNoOfTickets).sum();
        return totalNoOfTickets > Constants.MAX_TICKETS_ALLOWED;
    }

    /**
     * This method validates
     * 1. If atleast one adult is booking the tickets
     *
     * @param ticketTypeRequests
     * @return boolean value of validation
     */
    private boolean isAdultTicketPresent(TicketTypeRequest... ticketTypeRequests) {
        return Arrays.stream(ticketTypeRequests).anyMatch(e -> e.getTicketType().equals(TicketTypeRequest.Type.ADULT));
    }
}