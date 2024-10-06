import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;

import io.prometheus.client.CollectorRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import thirdparty.paymentgateway.TicketPaymentService;
import thirdparty.seatbooking.SeatReservationService;
import uk.gov.dwp.uc.pairtest.TicketServiceImpl;
import uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest;
import uk.gov.dwp.uc.pairtest.enums.ErrorCodes;
import uk.gov.dwp.uc.pairtest.exception.InvalidPurchaseException;

@ExtendWith(MockitoExtension.class)
class TicketServiceImplTest {
    @InjectMocks private TicketServiceImpl ticketService;
    @Mock private SeatReservationService seatReservationService;
    @Mock private TicketPaymentService ticketPaymentService;
    @BeforeEach
    @AfterEach
    public void cleanup() {
        CollectorRegistry.defaultRegistry.clear();
    }
    @Test
    void givenNullAccountId_thenFailed() {
        TicketTypeRequest ticketTypeRequest = new TicketTypeRequest(TicketTypeRequest.Type.ADULT,1);
        InvalidPurchaseException invalidPurchaseException =
                assertThrows(
                        InvalidPurchaseException.class,
                        () -> ticketService.purchaseTickets(null,ticketTypeRequest));
        assertNotNull(invalidPurchaseException);
        assertEquals(ErrorCodes.ERRORCT02.name(), invalidPurchaseException.getErrorCode().name());
        assertEquals(ErrorCodes.ERRORCT02.getMsg(), invalidPurchaseException.getErrorCode().getMsg());
    }

    @Test
    void givenZeroAccountId_thenFailed() {
        TicketTypeRequest ticketTypeRequest = new TicketTypeRequest(TicketTypeRequest.Type.ADULT,1);
        InvalidPurchaseException invalidPurchaseException =
                assertThrows(
                        InvalidPurchaseException.class,
                        () -> ticketService.purchaseTickets(0L,ticketTypeRequest));
        assertNotNull(invalidPurchaseException);
        assertEquals(ErrorCodes.ERRORCT02.name(), invalidPurchaseException.getErrorCode().name());
        assertEquals(ErrorCodes.ERRORCT02.getMsg(), invalidPurchaseException.getErrorCode().getMsg());
    }

    @Test
    void givenTotalTicketCountMoreThanMaxAllowed_thenFailed() {
        TicketTypeRequest ticketTypeRequest = new TicketTypeRequest(TicketTypeRequest.Type.ADULT,21);
        InvalidPurchaseException invalidPurchaseException =
                assertThrows(
                        InvalidPurchaseException.class,
                        () -> ticketService.purchaseTickets(1L, ticketTypeRequest));
        assertNotNull(invalidPurchaseException);
        assertEquals(ErrorCodes.ERRORCT03.name(), invalidPurchaseException.getErrorCode().name());
        assertEquals(ErrorCodes.ERRORCT03.getMsg(), invalidPurchaseException.getErrorCode().getMsg());
    }

    @Test
    void givenNoAdultTicket_thenFailed() {
        TicketTypeRequest ticketTypeRequest = new TicketTypeRequest(TicketTypeRequest.Type.CHILD,20);
        InvalidPurchaseException invalidPurchaseException =
                assertThrows(
                        InvalidPurchaseException.class,
                        () -> ticketService.purchaseTickets(1L,ticketTypeRequest));
        assertNotNull(invalidPurchaseException);
        assertEquals(ErrorCodes.ERRORCT04.name(), invalidPurchaseException.getErrorCode().name());
        assertEquals(ErrorCodes.ERRORCT04.getMsg(), invalidPurchaseException.getErrorCode().getMsg());
    }

    @Test
    void givenPurchaseDataValid_thenSucceed() {
        TicketTypeRequest ticketTypeRequestAdult = new TicketTypeRequest(TicketTypeRequest.Type.ADULT,10);
        TicketTypeRequest ticketTypeRequestChild = new TicketTypeRequest(TicketTypeRequest.Type.CHILD,10);
        TicketTypeRequest ticketTypeRequestInfant = new TicketTypeRequest(TicketTypeRequest.Type.INFANT,10);
        ticketService.purchaseTickets(1L, ticketTypeRequestAdult, ticketTypeRequestChild, ticketTypeRequestInfant);
        Mockito.verify(seatReservationService).reserveSeat(1L, 20);
        Mockito.verify(ticketPaymentService).makePayment(1L, 300);
    }

    @Test
    void givenZeroTicket_thenFailed() {
        TicketTypeRequest ticketTypeRequestInvalid = new TicketTypeRequest(TicketTypeRequest.Type.CHILD,0);
        InvalidPurchaseException invalidPurchaseException =
                assertThrows(
                        InvalidPurchaseException.class,
                        () -> ticketService.purchaseTickets(1L, ticketTypeRequestInvalid));
        assertEquals(ErrorCodes.ERRORCT04.name(), invalidPurchaseException.getErrorCode().name());
        assertEquals(ErrorCodes.ERRORCT04.getMsg(), invalidPurchaseException.getErrorCode().getMsg());
    }

    @Test
    void givenTicketPaymentServiceThrowError_thenFailed() {
        doThrow(new InvalidPurchaseException(ErrorCodes.ERRORCT01, "Payment Failed!!"))
                .when(ticketPaymentService)
                .makePayment(1L, 20);
        TicketTypeRequest ticketTypeRequestInvalid = new TicketTypeRequest(TicketTypeRequest.Type.ADULT,1);
        InvalidPurchaseException invalidPurchaseException =
                assertThrows(
                        InvalidPurchaseException.class,
                        () -> ticketService.purchaseTickets(1L, ticketTypeRequestInvalid));
        assertEquals(ErrorCodes.ERRORCT01.name(), invalidPurchaseException.getErrorCode().name());
        assertEquals("Payment Failed!!", invalidPurchaseException.getMessage());
    }

    @Test
    void givenSeatReservationServiceThrowError_whenPurchaseTicket_thenFailed() {
        doThrow(new InvalidPurchaseException(ErrorCodes.ERRORCT01, "Seat Reservation Failed!!"))
                .when(seatReservationService)
                .reserveSeat(1L, 20);
        TicketTypeRequest ticketTypeRequestInvalid = new TicketTypeRequest(TicketTypeRequest.Type.ADULT,20);
        InvalidPurchaseException invalidPurchaseException =
                assertThrows(
                        InvalidPurchaseException.class,
                        () -> ticketService.purchaseTickets(1L, ticketTypeRequestInvalid));
        assertEquals(ErrorCodes.ERRORCT01.name(), invalidPurchaseException.getErrorCode().name());
        assertEquals("Seat Reservation Failed!!", invalidPurchaseException.getMessage());
    }

    @Test
    void givenAdultTicketLessThanInfant_thenFailed() {
        TicketTypeRequest ticketTypeRequestInfant = new TicketTypeRequest(TicketTypeRequest.Type.INFANT,19);
        TicketTypeRequest ticketTypeRequestAdult = new TicketTypeRequest(TicketTypeRequest.Type.ADULT,1);
        InvalidPurchaseException invalidPurchaseException =
                assertThrows(
                        InvalidPurchaseException.class,
                        () -> ticketService.purchaseTickets(1L, ticketTypeRequestInfant, ticketTypeRequestAdult));
        assertEquals(ErrorCodes.ERRORCT04.name(), invalidPurchaseException.getErrorCode().name());
        assertEquals(ErrorCodes.ERRORCT04.getMsg(), invalidPurchaseException.getErrorCode().getMsg());
    }
}