package com.carrental;

import com.carrental.enums.CarType;
import com.carrental.enums.ReservationStatus;
import com.carrental.model.Reservation;
import com.carrental.model.Sedan;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

//
//  Unit tests for Reservation domain logic — no Spring context needed.
//
//  Verifies:
//    - Reservation captures type, date/time, and number of days
//    - Drop-off is correctly computed from pickup + numberOfDays
//    - Overlap detection is correct (the key to enforcing fleet limits)
//    - State transitions are guarded (cancel, complete)
//
@DisplayName("Reservation captures type, date/time, and number of days")
class ReservationDomainTest {

    private static final LocalDateTime BASE =
            LocalDateTime.of(2026, 6, 1, 10, 0);
    private Sedan sedan;

    @BeforeEach
    void setUp() {
        sedan = new Sedan("S-1", "Toyota", "Camry", 2023);
    }

    private Reservation buildReservation(LocalDateTime pickup, int days) {
        return Reservation.builder()
                .customerName("Prashanth")
                .customerEmail("prashanth@test.com")
                .carType(CarType.SEDAN)
                .car(sedan)
                .pickupDateTime(pickup)
                .numberOfDays(days)
                .status(ReservationStatus.CONFIRMED)
                .totalCost(sedan.calculateTotalRentalCost(days))
                .build();
    }

    // TEst  reservation attributes
    @Test
    @DisplayName("Reservation stores car type, pickup date/time, and number of days")
    void testReservationStoresCorrectAttributes() {
        Reservation r = buildReservation(BASE, 4);

        assertEquals(CarType.SEDAN, r.getCarType());
        assertEquals(BASE, r.getPickupDateTime());
        assertEquals(4, r.getNumberOfDays());
        assertEquals(ReservationStatus.CONFIRMED, r.getStatus());
    }

    @Test
    @DisplayName("Drop-off date = pickup + numberOfDays")
    void testReservationDropOffIsPickupPlusDays() {
        Reservation r = buildReservation(BASE, 3);
        assertEquals(BASE.plusDays(3), r.getDropOffDateTime());
    }

    @Test
    @DisplayName("Total cost = dailyRate × numberOfDays")
    void testReservationTotalCostMatchesDailyRateTimeDays() {
        Reservation r = buildReservation(BASE, 5);
        BigDecimal expected = sedan.getDailyRentalRate().multiply(BigDecimal.valueOf(5));
        assertEquals(0, r.getTotalCost().compareTo(expected));
    }

    // Test Overlap time detection
    // Reservation: Jun 5 → Jun 10 (5 days)
    @Test
    @DisplayName("Overlaps: exact same window")
    void testOverlapExactSameWindow() {
        Reservation r = buildReservation(BASE.plusDays(4), 5); // Jun 5–10
        assertTrue(r.overlapsWith(BASE.plusDays(4), BASE.plusDays(9)));
    }

    @Test
    @DisplayName("Overlaps: new window starts before, ends during")
    void testOverlapNewStartsBeforeEndsInside() {
        Reservation r = buildReservation(BASE.plusDays(4), 5); // Jun 5–10
        assertTrue(r.overlapsWith(BASE.plusDays(2), BASE.plusDays(7))); // Jun 3–7 overlaps
    }

    @Test
    @DisplayName("Overlaps: new window starts during, ends after")
    void testOverlapNewStartsInsideEndsAfter() {
        Reservation r = buildReservation(BASE.plusDays(4), 5); // Jun 5–10
        assertTrue(r.overlapsWith(BASE.plusDays(7), BASE.plusDays(12))); // Jun 7–12 overlaps
    }

    @Test
    @DisplayName("Overlaps: new window contained entirely inside")
    void testOverlapNewWindowContainedInside() {
        Reservation r = buildReservation(BASE.plusDays(4), 5); // Jun 5–10
        assertTrue(r.overlapsWith(BASE.plusDays(5), BASE.plusDays(8))); // Jun 6–8 overlaps
    }

    @Test
    @DisplayName("No overlap: new window ends exactly when reservation starts")
    void testNoOverlapNewWindowEndsExactlyAtStart() {
        Reservation r = buildReservation(BASE.plusDays(4), 5); // Jun 5–10
        assertFalse(r.overlapsWith(BASE, BASE.plusDays(4)));   // Jun 1–5: ends exactly at start
    }

    @Test
    @DisplayName("No overlap: new window starts exactly when reservation ends")
    void testNoOverlapNewWindowStartsAtDropOff() {
        Reservation r = buildReservation(BASE.plusDays(4), 5); // Jun 5–10
        assertFalse(r.overlapsWith(BASE.plusDays(9), BASE.plusDays(14))); // Jun 10–15: starts at drop-off
    }

    @Test
    @DisplayName("No overlap: new window entirely before")
    void testNoOverlapWindowEntirelyBefore() {
        Reservation r = buildReservation(BASE.plusDays(4), 5); // Jun 5–10
        assertFalse(r.overlapsWith(BASE, BASE.plusDays(3)));   // Jun 1–3: entirely before
    }

    @Test
    @DisplayName("No overlap: new window entirely after")
    void testNoOverlapWindowEntirelyAfter() {
        Reservation r = buildReservation(BASE.plusDays(4), 5); // Jun 5–10
        assertFalse(r.overlapsWith(BASE.plusDays(10), BASE.plusDays(14))); // Jun 10–14: entirely after
    }

    // Test Reservation State transitions
    @Test
    @DisplayName("Cancel moves status to CANCELLED")
    void testCancelMovesToCancelled() {
        Reservation r = buildReservation(BASE, 3);
        r.cancel();
        assertEquals(ReservationStatus.CANCELLED, r.getStatus());
        assertFalse(r.isActive());
    }

    @Test
    @DisplayName("Complete moves status to COMPLETED")
    void testCompleteMovesToCompleted() {
        Reservation r = buildReservation(BASE, 3);
        r.complete();
        assertEquals(ReservationStatus.COMPLETED, r.getStatus());
        assertFalse(r.isActive());
    }

    @Test
    @DisplayName("Cannot cancel an already-cancelled reservation")
    void testCancelTwiceThrowsException() {
        Reservation r = buildReservation(BASE, 3);
        r.cancel();
        assertThrows(IllegalStateException.class,
                () -> {
                    r.complete();
                });
    }

    @Test
    @DisplayName("Cannot complete an already-cancelled reservation")
    void testCompleteCancelledThrowsException() {
        Reservation r = buildReservation(BASE, 3);
        r.cancel();
        assertThrows(IllegalStateException.class,
                () -> {
                    r.complete();
                });
    }

    @Test
    @DisplayName("isActive returns true only for CONFIRMED status")
    void testIsActiveOnlyWhenConfirmed() {
        Reservation r = buildReservation(BASE, 3);
        assertTrue(r.isActive());
        r.cancel();
        assertFalse(r.isActive());
    }
}
